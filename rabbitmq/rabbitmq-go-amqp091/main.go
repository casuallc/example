package main

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"rabbitmq-go-amqp091/rabbit"
)

type sendMessageRequest struct {
	Message string `json:"Message"`
}

func main() {
	configPath := flag.String("config", "config.json", "path to the JSON config file")
	flag.Parse()

	opts, err := rabbit.Load(*configPath)
	if err != nil {
		log.Fatalf("Load config: %v", err)
	}
	conn := rabbit.NewConnection(opts)
	defer conn.Close()

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	go func() {
		if err := rabbit.Consume(ctx, conn, opts); err != nil {
			log.Printf("Consumer stopped: %v", err)
		}
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("POST /api/messages", func(w http.ResponseWriter, r *http.Request) {
		var req sendMessageRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "invalid JSON body", http.StatusBadRequest)
			return
		}
		if err := conn.Publish(r.Context(), req.Message); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]string{"status": "sent", "Message": req.Message})
	})

	server := &http.Server{Addr: opts.HTTPAddr, Handler: mux}
	go func() {
		<-ctx.Done()
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = server.Shutdown(shutdownCtx)
	}()

	log.Printf("HTTP listening on %s", opts.HTTPAddr)
	if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatalf("HTTP server: %v", err)
	}
}
