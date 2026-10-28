package com.apusic.admq.example.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 生产者线程：负责一批队列，通过默认（匿名）交换机 "" 发布消息，
 * routing key = 队列名，消息直接路由到同名队列。
 */
public class ProducerWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ProducerWorker.class);

    private final int workerId;
    private final List<String> queues;
    private final ConnectionPool pool;
    private final PerfConfig cfg;
    private final Metrics metrics;
    private final RateLimiter rateLimiter;
    private final AtomicBoolean running;

    public ProducerWorker(int workerId, List<String> queues, ConnectionPool pool,
                          PerfConfig cfg, Metrics metrics, RateLimiter rateLimiter,
                          AtomicBoolean running) {
        this.workerId = workerId;
        this.queues = queues;
        this.pool = pool;
        this.cfg = cfg;
        this.metrics = metrics;
        this.rateLimiter = rateLimiter;
        this.running = running;
    }

    @Override
    public void run() {
        while (running.get()) {
            Channel channel = null;
            try {
                channel = pool.createChannel();
                // 默认交换机：exchange = ""，routing key 即队列名
                final Channel ch = channel;
                while (running.get() && ch.isOpen()) {
                    for (String queue : queues) {
                        if (!running.get()) {
                            break;
                        }
                        rateLimiter.acquire();
                        publish(ch, queue);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (running.get()) {
                    log.warn("producer-{} 异常，1 秒后重建 channel: {}", workerId, e.toString());
                    sleepQuietly(1000);
                }
            } finally {
                closeQuietly(channel);
            }
        }
    }

    private void publish(Channel channel, String queue) throws IOException {
        byte[] body = buildMessage();
        AMQP.BasicProperties props = cfg.persistent
                ? MessageProperties.PERSISTENT_BASIC
                : MessageProperties.BASIC;
        try {
            // 匿名（默认）交换机 + routing key = 队列名 -> 直达同名队列
            channel.basicPublish("", queue, props, body);
            metrics.sent.incrementAndGet();
        } catch (IOException e) {
            metrics.sentFailed.incrementAndGet();
            throw e;
        }
    }

    private byte[] buildMessage() {
        byte[] body = new byte[cfg.messageSize];
        if (cfg.measureLatency && cfg.messageSize >= 8) {
            ByteBuffer.wrap(body).putLong(System.currentTimeMillis());
        }
        return body;
    }

    private void closeQuietly(Channel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
