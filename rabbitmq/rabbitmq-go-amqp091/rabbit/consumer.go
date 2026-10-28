package rabbit

import (
	"context"
	"errors"
	"fmt"
	"log"
	"time"
)

// Consume runs the consumer loop on the shared demo queue until ctx is
// canceled. It declares the topology itself (idempotent) so the sample works
// even if nothing has published yet. On connection loss it retries with a
// fixed backoff.
func Consume(ctx context.Context, conn *Connection, opts Options) error {
	for {
		err := consumeOnce(ctx, conn, opts)
		if err == nil || ctx.Err() != nil {
			return nil
		}
		log.Printf("Consumer error: %v, retrying in 5s", err)
		select {
		case <-ctx.Done():
			return nil
		case <-time.After(5 * time.Second):
		}
	}
}

func consumeOnce(ctx context.Context, conn *Connection, opts Options) error {
	if err := conn.EnsureInitialized(); err != nil {
		return err
	}
	ch, err := conn.NewChannel()
	if err != nil {
		return err
	}
	defer func() { _ = ch.Close() }()
	if err := DeclareTopology(ch, opts); err != nil {
		return err
	}

	deliveries, err := ch.Consume(opts.Queue, "", false, false, false, false, nil)
	if err != nil {
		return fmt.Errorf("consume queue %s: %w", opts.Queue, err)
	}
	log.Printf("Consuming queue=%s bound to exchange=%s routingKey=%s", opts.Queue, opts.Exchange, opts.RoutingKey)

	for {
		select {
		case <-ctx.Done():
			return nil
		case d, ok := <-deliveries:
			if !ok {
				return errors.New("delivery channel closed")
			}
			log.Printf("Received message: %s", d.Body)
			if err := d.Ack(false); err != nil {
				return fmt.Errorf("ack: %w", err)
			}
		}
	}
}
