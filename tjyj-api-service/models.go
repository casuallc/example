package main

// Downstream models (EMQX 5.x HTTP API).

// EmqxTopicMetrics represents data from GET /api/v5/mqtt/topic_metrics/:topic.
type EmqxTopicMetrics struct {
	Topic      string `json:"topic"`
	CreateTime string `json:"create_time"`
	Metrics    struct {
		MessagesInCount      int64   `json:"messages.in.count"`
		MessagesOutCount     int64   `json:"messages.out.count"`
		MessagesDroppedCount int64   `json:"messages.dropped.count"`
		MessagesInRate       float64 `json:"messages.in.rate"`
		MessagesOutRate      float64 `json:"messages.out.rate"`
	} `json:"metrics"`
}

// EmqxSubscription is a single subscription record from /api/v5/subscriptions.
type EmqxSubscription struct {
	ClientID string `json:"clientid"`
	Topic    string `json:"topic"`
	Qos      int    `json:"qos"`
	Node     string `json:"node"`
}

// EmqxSubscriptionsResponse is the paginated list from /api/v5/subscriptions.
type EmqxSubscriptionsResponse struct {
	Data []EmqxSubscription `json:"data"`
	Meta struct {
		Page    int  `json:"page"`
		Limit   int  `json:"limit"`
		Count   int  `json:"count"`
		HasNext bool `json:"hasnext"`
	} `json:"meta"`
}

// EmqxClientInfo represents data from GET /api/v5/clients/:clientid.
type EmqxClientInfo struct {
	ClientID    string `json:"clientid"`
	Username    string `json:"username"`
	IPAddress   string `json:"ip_address"`
	Port        int    `json:"port"`
	Connected   bool   `json:"connected"`
	ConnectedAt string `json:"connected_at"`
	Node        string `json:"node"`
	SendMsg     int64  `json:"send_msg"`
	RecvMsg     int64  `json:"recv_msg"`
	InflightCnt int    `json:"inflight_cnt"`
}

// Upstream response models (Pulsar topic stats format).

type PulsarTopicStats struct {
	MsgInCounter  int64                       `json:"msgInCounter"`
	MsgOutCounter int64                       `json:"msgOutCounter"`
	Subscriptions map[string]SubscriptionInfo `json:"subscriptions"`
}

type SubscriptionInfo struct {
	MsgOutCounter   int64      `json:"msgOutCounter"`
	MsgBacklog      int64      `json:"msgBacklog"`
	BacklogSize     int64      `json:"backlogSize"`
	UnackedMessages int64      `json:"unackedMessages"`
	Consumers       []Consumer `json:"consumers"`
}

type Consumer struct {
	MsgOutCounter       int64  `json:"msgOutCounter"`
	ConsumerName        string `json:"consumerName"`
	UnackedMessages     int64  `json:"unackedMessages"`
	AvgMessagesPerEntry int    `json:"avgMessagesPerEntry"`
	Address             string `json:"address"`
	ConnectedSince      string `json:"connectedSince"`
}
