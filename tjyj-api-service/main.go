package main

import (
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

var (
	version   = "dev"
	buildTime = ""
)

func main() {
	cfg := LoadConfig()

	if cfg.EmqxAPIKey == "" || cfg.EmqxAPISecret == "" {
		log.Fatal("EMQX_API_KEY and EMQX_API_SECRET environment variables are required")
	}

	client := NewEmqxClient(cfg.EmqxBaseURL, cfg.EmqxAPIKey, cfg.EmqxAPISecret, cfg.EmqxTimeout)
	statsService := NewStatsService(client, cfg)
	healthService := NewHealthService(cfg.HealthPort, cfg.HealthTimeout)

	r := gin.Default()
	handler := NewHandler(statsService, healthService)
	handler.Register(r)

	srv := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
	}

	log.Printf("Server listening on %s", srv.Addr)
	if err := srv.ListenAndServe(); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
