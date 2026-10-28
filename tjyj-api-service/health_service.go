package main

import (
	"fmt"
	"net"
	"time"
)

// HealthService checks whether a local TCP port is listening.
type HealthService struct {
	port    int
	timeout time.Duration
}

// NewHealthService creates a new HealthService.
func NewHealthService(port int, timeout time.Duration) *HealthService {
	return &HealthService{port: port, timeout: timeout}
}

// Check returns "ok" if the configured port is listening, otherwise "failed".
func (s *HealthService) Check() string {
	addr := fmt.Sprintf("127.0.0.1:%d", s.port)
	conn, err := net.DialTimeout("tcp", addr, s.timeout)
	if err != nil {
		return "failed"
	}
	_ = conn.Close()
	return "ok"
}
