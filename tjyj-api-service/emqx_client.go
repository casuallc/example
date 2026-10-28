package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/url"
	"time"
)

// ErrTopicMetricsNotFound is returned when a topic has no registered topic metrics.
var ErrTopicMetricsNotFound = errors.New("topic metrics not registered")

// EmqxClient calls the EMQX 5.x HTTP API with API key/secret basic auth.
type EmqxClient struct {
	baseURL   string
	apiKey    string
	apiSecret string
	http      *http.Client
}

// NewEmqxClient creates a new client with the given base URL, credentials and request timeout.
func NewEmqxClient(baseURL, apiKey, apiSecret string, timeout time.Duration) *EmqxClient {
	return &EmqxClient{
		baseURL:   baseURL,
		apiKey:    apiKey,
		apiSecret: apiSecret,
		http:      &http.Client{Timeout: timeout},
	}
}

// topicMetricsURL builds the URL for a single topic's metrics endpoint.
// The topic is fully URL-escaped ("/" -> "%2F") as EMQX expects.
func (c *EmqxClient) topicMetricsURL(topic string) string {
	return fmt.Sprintf("%s/api/v5/mqtt/topic_metrics/%s", c.baseURL, url.PathEscape(topic))
}

// CreateTopicMetrics registers metrics collection for a topic.
// If the topic is already registered, EMQX returns an error which is treated as success.
func (c *EmqxClient) CreateTopicMetrics(ctx context.Context, topic string) error {
	// EMQX 5.x creates topic metrics via POST /mqtt/topic_metrics with a JSON body;
	// POST to the per-topic path is not supported (405).
	u := fmt.Sprintf("%s/api/v5/mqtt/topic_metrics", c.baseURL)
	err := c.do(ctx, http.MethodPost, u, map[string]string{"topic": topic}, nil)
	if err != nil {
		var apiErr *emqxAPIError
		// EMQX returns an error ("already_existed") when the topic is already
		// registered; treat it as success so creation is idempotent.
		if errors.As(err, &apiErr) && (apiErr.status == http.StatusBadRequest || apiErr.status == http.StatusConflict) {
			slog.Info("topic metrics already registered", "topic", topic)
			return nil
		}
		return err
	}
	return nil
}

// GetTopicMetrics fetches metrics for a single topic.
// Returns ErrTopicMetricsNotFound if the topic is not registered (404).
func (c *EmqxClient) GetTopicMetrics(ctx context.Context, topic string) (*EmqxTopicMetrics, error) {
	u := c.topicMetricsURL(topic)
	var metrics EmqxTopicMetrics
	if err := c.do(ctx, http.MethodGet, u, nil, &metrics); err != nil {
		var apiErr *emqxAPIError
		if errors.As(err, &apiErr) && apiErr.status == http.StatusNotFound {
			return nil, ErrTopicMetricsNotFound
		}
		return nil, err
	}
	return &metrics, nil
}

// DeleteTopicMetrics removes metrics collection for a topic.
func (c *EmqxClient) DeleteTopicMetrics(ctx context.Context, topic string) error {
	return c.do(ctx, http.MethodDelete, c.topicMetricsURL(topic), nil, nil)
}

// ListTopicMetrics lists all registered topic metrics.
func (c *EmqxClient) ListTopicMetrics(ctx context.Context) ([]EmqxTopicMetrics, error) {
	u := fmt.Sprintf("%s/api/v5/mqtt/topic_metrics", c.baseURL)
	var metrics []EmqxTopicMetrics
	if err := c.do(ctx, http.MethodGet, u, nil, &metrics); err != nil {
		return nil, err
	}
	return metrics, nil
}

// GetSubscriptions fetches a page of subscriptions for a topic.
func (c *EmqxClient) GetSubscriptions(ctx context.Context, topic string, page, limit int) (*EmqxSubscriptionsResponse, error) {
	u := fmt.Sprintf("%s/api/v5/subscriptions?topic=%s&page=%d&limit=%d",
		c.baseURL, url.QueryEscape(topic), page, limit)
	var res EmqxSubscriptionsResponse
	if err := c.do(ctx, http.MethodGet, u, nil, &res); err != nil {
		return nil, err
	}
	return &res, nil
}

// GetClient fetches connection details for a single client.
func (c *EmqxClient) GetClient(ctx context.Context, clientID string) (*EmqxClientInfo, error) {
	u := fmt.Sprintf("%s/api/v5/clients/%s", c.baseURL, url.PathEscape(clientID))
	var info EmqxClientInfo
	if err := c.do(ctx, http.MethodGet, u, nil, &info); err != nil {
		return nil, err
	}
	return &info, nil
}

// emqxAPIError is a non-2xx response from EMQX.
type emqxAPIError struct {
	status int
	body   string
}

func (e *emqxAPIError) Error() string {
	return fmt.Sprintf("unexpected status code: %d, body: %s", e.status, truncate(e.body, 200))
}

// do performs an authenticated request. reqBody, if non-nil, is JSON-encoded as
// the request body. If dest is non-nil, the response body is JSON-decoded into
// it; otherwise the body is drained and discarded.
func (c *EmqxClient) do(ctx context.Context, method, u string, reqBody, dest any) error {
	slog.Info("downstream request", "method", method, "url", u)
	var bodyReader io.Reader
	if reqBody != nil {
		data, err := json.Marshal(reqBody)
		if err != nil {
			return fmt.Errorf("encode request body: %w", err)
		}
		bodyReader = bytes.NewReader(data)
	}
	req, err := http.NewRequestWithContext(ctx, method, u, bodyReader)
	if err != nil {
		return fmt.Errorf("build request: %w", err)
	}
	req.SetBasicAuth(c.apiKey, c.apiSecret)
	if reqBody != nil {
		req.Header.Set("Content-Type", "application/json")
	}

	resp, err := c.http.Do(req)
	if err != nil {
		return fmt.Errorf("do request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("read response body: %w", err)
	}
	slog.Info("downstream response", "method", method, "url", u, "status", resp.StatusCode, "body_length", len(body))

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return &emqxAPIError{status: resp.StatusCode, body: string(body)}
	}
	if dest == nil || len(body) == 0 {
		return nil
	}
	if err := json.Unmarshal(body, dest); err != nil {
		return fmt.Errorf("decode response: %w, body: %s", err, truncate(string(body), 200))
	}
	return nil
}

// truncate shortens s to at most n bytes for logging/error messages.
func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "..."
}
