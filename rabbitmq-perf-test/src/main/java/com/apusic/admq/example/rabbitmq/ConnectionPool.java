package com.apusic.admq.example.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接池：固定数量 TCP 连接，channel 在连接上轮询创建，实现连接复用。
 */
public class ConnectionPool implements AutoCloseable {

    private final List<Connection> connections = new ArrayList<Connection>();
    private final AtomicInteger cursor = new AtomicInteger();

    public ConnectionPool(PerfConfig cfg) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.host);
        factory.setPort(cfg.port);
        factory.setVirtualHost(cfg.vhost);
        factory.setUsername(cfg.username);
        factory.setPassword(cfg.password);
        factory.setConnectionTimeout(cfg.connectionTimeoutMs);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        // 心跳，及时发现服务端故障
        factory.setRequestedHeartbeat(30);

        for (int i = 0; i < cfg.connectionCount; i++) {
            Connection conn = factory.newConnection("perf-test-conn-" + i);
            connections.add(conn);
        }
    }

    /** 在连接上轮询创建 channel */
    public Channel createChannel() throws IOException {
        int idx = Math.floorMod(cursor.getAndIncrement(), connections.size());
        return connections.get(idx).createChannel();
    }

    public int connectionCount() {
        return connections.size();
    }

    @Override
    public void close() {
        for (Connection conn : connections) {
            try {
                if (conn.isOpen()) {
                    conn.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
