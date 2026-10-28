package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"sync"
)

// StatsService aggregates EMQX monitoring data into Pulsar topic stats format.
type StatsService struct {
	client *EmqxClient
	cfg    Config
}

// NewStatsService creates a new StatsService.
func NewStatsService(client *EmqxClient, cfg Config) *StatsService {
	return &StatsService{client: client, cfg: cfg}
}

// GetTopicStats builds the Pulsar-compatible stats response for the given topic path.
func (s *StatsService) GetTopicStats(ctx context.Context, tenant, namespace, topic string) (*PulsarTopicStats, error) {
	// EMQX expects the raw MQTT topic name (e.g. home/warn/sub/1), not the full Pulsar path.
	mqttTopic := topic

	// Message counters come from EMQX topic metrics when the topic is registered.
	// Topic metrics only count from registration time, so an unregistered topic
	// falls back to per-client send_msg/recv_msg aggregation below.
	var msgIn, msgOut int64
	metricsFound := false
	metrics, err := s.client.GetTopicMetrics(ctx, mqttTopic)
	switch {
	case err == nil:
		metricsFound = true
		msgIn = metrics.Metrics.MessagesInCount
		msgOut = metrics.Metrics.MessagesOutCount
	case errors.Is(err, ErrTopicMetricsNotFound):
		slog.Info("topic metrics not registered, falling back to per-client aggregation", "topic", mqttTopic)
	default:
		return nil, fmt.Errorf("get topic metrics: %w", err)
	}

	subscriptions, err := s.fetchAllSubscriptions(ctx, mqttTopic)
	if err != nil {
		return nil, fmt.Errorf("get subscriptions: %w", err)
	}

	consumers, totalSendMsg, totalRecvMsg, totalUnacked := s.fetchConsumerDetails(ctx, subscriptions)
	if consumers == nil {
		consumers = make([]Consumer, 0)
	}

	if !metricsFound {
		msgIn = totalRecvMsg
		msgOut = totalSendMsg
	}

	return &PulsarTopicStats{
		MsgInCounter:  msgIn,
		MsgOutCounter: msgOut,
		Subscriptions: map[string]SubscriptionInfo{
			topic: {
				MsgOutCounter:   msgOut,
				MsgBacklog:      0,
				BacklogSize:     0,
				UnackedMessages: totalUnacked,
				Consumers:       consumers,
			},
		},
	}, nil
}

func (s *StatsService) fetchAllSubscriptions(ctx context.Context, topic string) ([]EmqxSubscription, error) {
	var all []EmqxSubscription
	page := 1
	for {
		data, err := s.client.GetSubscriptions(ctx, topic, page, s.cfg.EmqxPageSize)
		if err != nil {
			return nil, err
		}
		slog.Debug("fetched subscriptions page", "page", page, "count", len(data.Data), "hasnext", data.Meta.HasNext)
		all = append(all, data.Data...)
		if !data.Meta.HasNext || len(data.Data) == 0 {
			break
		}
		page++
	}
	slog.Info("total subscriptions fetched", "count", len(all), "topic", topic)
	return all, nil
}

func (s *StatsService) fetchConsumerDetails(ctx context.Context, subs []EmqxSubscription) ([]Consumer, int64, int64, int64) {
	var mu sync.Mutex
	consumers := make([]Consumer, 0, len(subs))
	var totalSendMsg int64
	var totalRecvMsg int64
	var totalUnacked int64

	concurrency := s.cfg.EmqxMaxConcurrency
	if concurrency <= 0 {
		concurrency = 1
	}
	sem := make(chan struct{}, concurrency)
	var wg sync.WaitGroup

	for _, sub := range subs {
		wg.Add(1)
		sem <- struct{}{}
		go func(clientID string) {
			defer wg.Done()
			defer func() { <-sem }()

			conn, err := s.client.GetClient(ctx, clientID)
			if err != nil {
				slog.Warn("failed to get client details", "clientId", clientID, "error", err)
				return
			}

			unacked := int64(conn.InflightCnt)
			c := Consumer{
				MsgOutCounter:       conn.SendMsg,
				ConsumerName:        clientID,
				UnackedMessages:     unacked,
				AvgMessagesPerEntry: 0,
				Address:             fmt.Sprintf("/%s:%d", conn.IPAddress, conn.Port),
				ConnectedSince:      conn.ConnectedAt,
			}
			mu.Lock()
			consumers = append(consumers, c)
			totalSendMsg += conn.SendMsg
			totalRecvMsg += conn.RecvMsg
			totalUnacked += unacked
			mu.Unlock()
		}(sub.ClientID)
	}
	wg.Wait()

	slog.Info("consumer details fetched", "consumers", len(consumers), "totalSendMsg", totalSendMsg, "totalRecvMsg", totalRecvMsg, "totalUnacked", totalUnacked)
	return consumers, totalSendMsg, totalRecvMsg, totalUnacked
}
