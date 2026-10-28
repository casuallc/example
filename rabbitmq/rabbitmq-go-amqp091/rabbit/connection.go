package rabbit

import (
	"context"
	"fmt"
	"log"
	"sync"

	amqp "github.com/rabbitmq/amqp091-go"
)

// Connection owns the shared RabbitMQ connection and the channel used for
// publishing. Initialization is lazy and idempotent so the consumer and the
// HTTP endpoint can start in any order.
type Connection struct {
	opts Options

	mu    sync.Mutex
	conn  *amqp.Connection
	pubCh *amqp.Channel
}

// NewConnection creates a not-yet-connected Connection.
func NewConnection(opts Options) *Connection {
	return &Connection{opts: opts}
}

// EnsureInitialized connects to RabbitMQ and declares the topology once.
func (c *Connection) EnsureInitialized() error {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.conn != nil && !c.conn.IsClosed() {
		return nil
	}
	uri := amqp.URI{
		Scheme:   "amqp",
		Host:     c.opts.Host,
		Port:     c.opts.Port,
		Username: c.opts.Username,
		Password: c.opts.Password,
		Vhost:    c.opts.VirtualHost,
	}.String()
	conn, err := amqp.Dial(uri)
	if err != nil {
		return fmt.Errorf("dial %s:%d vhost=%s: %w", c.opts.Host, c.opts.Port, c.opts.VirtualHost, err)
	}
	ch, err := conn.Channel()
	if err != nil {
		_ = conn.Close()
		return fmt.Errorf("open publish channel: %w", err)
	}
	if err := DeclareTopology(ch, c.opts); err != nil {
		_ = ch.Close()
		_ = conn.Close()
		return err
	}
	c.conn = conn
	c.pubCh = ch
	log.Printf("Connected to RabbitMQ %s:%d vhost=%s", c.opts.Host, c.opts.Port, c.opts.VirtualHost)
	return nil
}

// NewChannel opens a channel on the shared connection.
func (c *Connection) NewChannel() (*amqp.Channel, error) {
	if err := c.EnsureInitialized(); err != nil {
		return nil, err
	}
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.conn.Channel()
}

// DeclareTopology declares the shared demo topology; safe to call repeatedly.
func DeclareTopology(ch *amqp.Channel, opts Options) error {
	if err := ch.ExchangeDeclare(opts.Exchange, "topic", true, false, false, false, nil); err != nil {
		return fmt.Errorf("declare exchange %s: %w", opts.Exchange, err)
	}
	if _, err := ch.QueueDeclare(opts.Queue, true, false, false, false, nil); err != nil {
		return fmt.Errorf("declare queue %s: %w", opts.Queue, err)
	}
	if err := ch.QueueBind(opts.Queue, opts.RoutingKey, opts.Exchange, false, nil); err != nil {
		return fmt.Errorf("bind queue %s to exchange %s: %w", opts.Queue, opts.Exchange, err)
	}
	return nil
}

// Publish sends a UTF-8 text message to the demo exchange.
func (c *Connection) Publish(ctx context.Context, message string) error {
	if err := c.EnsureInitialized(); err != nil {
		return err
	}
	c.mu.Lock()
	defer c.mu.Unlock()
	err := c.pubCh.PublishWithContext(ctx, c.opts.Exchange, c.opts.RoutingKey, false, false, amqp.Publishing{
		ContentType:  "text/plain",
		DeliveryMode: amqp.Persistent,
		Body:         []byte(message),
	})
	if err != nil {
		return fmt.Errorf("publish: %w", err)
	}
	log.Printf("Sent message to exchange=%s routingKey=%s: %s", c.opts.Exchange, c.opts.RoutingKey, message)
	return nil
}

// Close shuts down the publish channel and the connection.
func (c *Connection) Close() {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.pubCh != nil {
		_ = c.pubCh.Close()
		c.pubCh = nil
	}
	if c.conn != nil {
		_ = c.conn.Close()
		c.conn = nil
	}
}
