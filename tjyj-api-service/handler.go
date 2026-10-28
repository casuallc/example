package main

import (
	"errors"
	"log/slog"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

// Handler exposes the upstream HTTP endpoints.
type Handler struct {
	statsService  *StatsService
	healthService *HealthService
}

// NewHandler creates a new Handler.
func NewHandler(stats *StatsService, health *HealthService) *Handler {
	return &Handler{statsService: stats, healthService: health}
}

// Register registers all routes on the given gin engine.
func (h *Handler) Register(r *gin.Engine) {
	r.GET("/admin/v2/persistent/:tenant/:namespace/*topic", h.Stats)
	r.GET("/admin/v2/brokers/health", h.Health)

	// Topic metrics management (proxied to EMQX 5.x mqtt/topic_metrics).
	r.POST("/api/v1/topic_metrics/*topic", h.CreateTopicMetrics)
	r.GET("/api/v1/topic_metrics/*topic", h.GetTopicMetrics)
	r.DELETE("/api/v1/topic_metrics/*topic", h.DeleteTopicMetrics)
	r.GET("/api/v1/topic_metrics", h.ListTopicMetrics)
}

// Stats handles the Pulsar-compatible topic stats endpoint.
// It supports multi-level topics, e.g.
//
//	/admin/v2/persistent/mqtt-public/default/home/warn/sub/1/stats
//
// where topic is "home/warn/sub/1".
func (h *Handler) Stats(c *gin.Context) {
	tenant := c.Param("tenant")
	namespace := c.Param("namespace")
	topic := c.Param("topic")

	// Strip leading slash from wildcard param.
	topic = strings.TrimPrefix(topic, "/")

	// The upstream path ends with /stats; topic is everything before it.
	if !strings.HasSuffix(topic, "/stats") {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid path, must end with /stats"})
		return
	}
	topic = strings.TrimSuffix(topic, "/stats")
	topic = strings.Trim(topic, "/")
	if topic == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "topic is empty"})
		return
	}

	slog.Info("stats request", "tenant", tenant, "namespace", namespace, "topic", topic)
	stats, err := h.statsService.GetTopicStats(c.Request.Context(), tenant, namespace, topic)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, stats)
}

// Health handles the brokers health endpoint.
func (h *Handler) Health(c *gin.Context) {
	status := h.healthService.Check()
	c.String(http.StatusOK, status)
}

// wildcardTopic extracts the multi-level topic from the /*topic wildcard param.
func wildcardTopic(c *gin.Context) (string, bool) {
	topic := strings.Trim(c.Param("topic"), "/")
	if topic == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "topic is empty"})
		return "", false
	}
	return topic, true
}

// CreateTopicMetrics registers metrics collection for a topic.
func (h *Handler) CreateTopicMetrics(c *gin.Context) {
	topic, ok := wildcardTopic(c)
	if !ok {
		return
	}
	slog.Info("create topic metrics", "topic", topic)
	if err := h.statsService.client.CreateTopicMetrics(c.Request.Context(), topic); err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": err.Error()})
		return
	}
	c.Status(http.StatusNoContent)
}

// GetTopicMetrics returns metrics for a single topic.
func (h *Handler) GetTopicMetrics(c *gin.Context) {
	topic, ok := wildcardTopic(c)
	if !ok {
		return
	}
	metrics, err := h.statsService.client.GetTopicMetrics(c.Request.Context(), topic)
	if err != nil {
		if errors.Is(err, ErrTopicMetricsNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": "topic metrics not registered"})
			return
		}
		c.JSON(http.StatusBadGateway, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, metrics)
}

// DeleteTopicMetrics removes metrics collection for a topic.
func (h *Handler) DeleteTopicMetrics(c *gin.Context) {
	topic, ok := wildcardTopic(c)
	if !ok {
		return
	}
	slog.Info("delete topic metrics", "topic", topic)
	if err := h.statsService.client.DeleteTopicMetrics(c.Request.Context(), topic); err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": err.Error()})
		return
	}
	c.Status(http.StatusNoContent)
}

// ListTopicMetrics lists all registered topic metrics.
func (h *Handler) ListTopicMetrics(c *gin.Context) {
	metrics, err := h.statsService.client.ListTopicMetrics(c.Request.Context())
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, metrics)
}
