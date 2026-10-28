package rabbit

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"strconv"
)

// Options holds connection and topology options. Defaults point at the test
// environment; Load overlays a JSON config file (if present) and then the
// RABBITMQ_HOST / RABBITMQ_PORT / RABBITMQ_USERNAME / RABBITMQ_PASSWORD /
// RABBITMQ_VHOST environment variables.
type Options struct {
	Host        string `json:"host"`
	Port        int    `json:"port"`
	Username    string `json:"username"`
	Password    string `json:"password"`
	VirtualHost string `json:"virtualHost"`
	Exchange    string `json:"exchange"`
	Queue       string `json:"queue"`
	RoutingKey  string `json:"routingKey"`
	HTTPAddr    string `json:"httpAddr"`
}

// DefaultOptions returns options pointing at the test environment.
func DefaultOptions() Options {
	return Options{
		Host:        "172.20.140.143",
		Port:        5672,
		Username:    "admq",
		Password:    "changeme",
		VirtualHost: "apusic",
		Exchange:    "demo.topic",
		Queue:       "demo.queue",
		RoutingKey:  "demo.message",
		HTTPAddr:    ":8080",
	}
}

// Load starts from the defaults, overlays the JSON config file when it exists,
// then applies environment variable overrides. A missing config file is not an
// error.
func Load(configPath string) (Options, error) {
	opts := DefaultOptions()

	data, err := os.ReadFile(configPath)
	switch {
	case err == nil:
		if err := json.Unmarshal(data, &opts); err != nil {
			return Options{}, fmt.Errorf("parse config file %s: %w", configPath, err)
		}
	case !errors.Is(err, fs.ErrNotExist):
		return Options{}, fmt.Errorf("read config file %s: %w", configPath, err)
	}

	if v := os.Getenv("RABBITMQ_HOST"); v != "" {
		opts.Host = v
	}
	if v := os.Getenv("RABBITMQ_PORT"); v != "" {
		port, err := strconv.Atoi(v)
		if err != nil {
			return Options{}, fmt.Errorf("invalid RABBITMQ_PORT %q: %w", v, err)
		}
		opts.Port = port
	}
	if v := os.Getenv("RABBITMQ_USERNAME"); v != "" {
		opts.Username = v
	}
	if v := os.Getenv("RABBITMQ_PASSWORD"); v != "" {
		opts.Password = v
	}
	if v := os.Getenv("RABBITMQ_VHOST"); v != "" {
		opts.VirtualHost = v
	}
	return opts, nil
}
