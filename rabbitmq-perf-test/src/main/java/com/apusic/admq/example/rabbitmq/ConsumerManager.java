package com.apusic.admq.example.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消费者管理器：为每个队列注册一个消费者，多个消费者共享同一个 channel，
 * channel 从连接池轮询创建，实现连接复用。
 */
public class ConsumerManager {

    private static final Logger log = LoggerFactory.getLogger(ConsumerManager.class);

    private final ConnectionPool pool;
    private final PerfConfig cfg;
    private final Metrics metrics;
    private final AtomicBoolean running;
    private final List<Channel> channels = new ArrayList<Channel>();

    public ConsumerManager(ConnectionPool pool, PerfConfig cfg, Metrics metrics, AtomicBoolean running) {
        this.pool = pool;
        this.cfg = cfg;
        this.metrics = metrics;
        this.running = running;
    }

    /** 为 queueCount 个队列批量注册消费者，返回注册成功数 */
    public int startConsumers() throws IOException {
        int perChannel = Math.max(1, cfg.consumersPerChannel);
        int registered = 0;
        Channel channel = null;
        int onCurrentChannel = 0;

        for (int i = 0; i < cfg.queueCount && running.get(); i++) {
            String queue = cfg.queueName(i);
            try {
                if (channel == null || onCurrentChannel >= perChannel || !channel.isOpen()) {
                    channel = pool.createChannel();
                    channels.add(channel);
                    channel.basicQos(cfg.consumerPrefetch);
                    onCurrentChannel = 0;
                }
                register(channel, queue);
                onCurrentChannel++;
                registered++;
                if (registered % 1000 == 0) {
                    log.info("已注册消费者 {}/{}", registered, cfg.queueCount);
                }
            } catch (IOException e) {
                metrics.receivedFailed.incrementAndGet();
                log.warn("注册消费者失败 queue={}: {}", queue, e.toString());
                channel = null; // channel 可能已关闭，下次循环重建
            }
        }
        log.info("消费者注册完成: {} 个队列, 使用 {} 个 channel, {} 个 TCP 连接",
                registered, channels.size(), pool.connectionCount());
        return registered;
    }

    private void register(Channel channel, final String queue) throws IOException {
        channel.basicConsume(queue, cfg.consumerAutoAck, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    if (cfg.measureLatency && body.length >= 8) {
                        long sentAt = ByteBuffer.wrap(body).getLong();
                        metrics.recordLatency((System.currentTimeMillis() - sentAt) * 1000L);
                    }
                    metrics.received.incrementAndGet();
                    if (!cfg.consumerAutoAck) {
                        getChannel().basicAck(envelope.getDeliveryTag(), false);
                    }
                } catch (Exception e) {
                    metrics.receivedFailed.incrementAndGet();
                    if (!cfg.consumerAutoAck && getChannel().isOpen()) {
                        try {
                            getChannel().basicNack(envelope.getDeliveryTag(), false, false);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });
    }

    public void close() {
        for (Channel channel : channels) {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (Exception ignored) {
            }
        }
        channels.clear();
    }
}
