package main

import (
	"os"
	"strconv"
	"time"
)

// Config holds all runtime configuration, loaded from environment variables.
type Config struct {
	ServerPort         string
	EmqxBaseURL        string
	EmqxAPIKey         string
	EmqxAPISecret      string
	EmqxTimeout        time.Duration
	EmqxPageSize       int
	EmqxMaxConcurrency int
	HealthPort         int
	HealthTimeout      time.Duration
}

// LoadConfig loads configuration with sensible defaults.
func LoadConfig() Config {
	return Config{
		ServerPort:         getEnv("SERVER_PORT", "8080"),
		EmqxBaseURL:        getEnv("EMQX_BASE_URL", "http://127.0.0.1:18083"),
		EmqxAPIKey:         getEnv("EMQX_API_KEY", ""),
		EmqxAPISecret:      getEnv("EMQX_API_SECRET", ""),
		EmqxTimeout:        parseDuration(getEnv("EMQX_TIMEOUT", "10s")),
		EmqxPageSize:       parseInt(getEnv("EMQX_PAGE_SIZE", "500")),
		EmqxMaxConcurrency: parseInt(getEnv("EMQX_MAX_CONCURRENCY", "20")),
		HealthPort:         parseInt(getEnv("HEALTH_PORT", "5682")),
		HealthTimeout:      parseDuration(getEnv("HEALTH_TIMEOUT", "2s")),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func parseInt(s string) int {
	v, err := strconv.Atoi(s)
	if err != nil {
		return 0
	}
	return v
}

func parseDuration(s string) time.Duration {
	d, err := time.ParseDuration(s)
	if err != nil {
		return 10 * time.Second
	}
	return d
}
