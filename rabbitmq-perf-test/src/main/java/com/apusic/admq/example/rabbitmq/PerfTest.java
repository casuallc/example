package com.apusic.admq.example.rabbitmq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RabbitMQ 队列规模压测工具。
 *
 * 模型：queueCount 个队列，每个队列 1 个生产者（轮询发送）+ 1 个消费者，
 * 消息通过默认（匿名）交换机 "" 发布，routing key = 队列名，直达同名队列。
 * 所有 channel 复用固定数量的 TCP 连接。
 *
 * 用法：
 *   java -jar rabbitmq-perf-test-1.0.0-jar-with-dependencies.jar \
 *        [--config=config.properties] [key=value ...]
 *
 * 常用参数（均可通过配置文件或命令行覆盖）：
 *   host=192.168.1.10 port=5672 vhost=/ username=admin password=secret
 *   queueCount=10000 queuePrefix=perf-queue- connectionCount=8
 *   producerThreads=16 producerRate=50000 messageSize=1024
 *   durationSeconds=300
 */
public class PerfTest {

    private static final Logger log = LoggerFactory.getLogger(PerfTest.class);

    public static void main(String[] args) throws Exception {
        final PerfConfig cfg = PerfConfig.load(args);
        log.info("压测配置: {}", cfg);

        final Metrics metrics = new Metrics();
        final AtomicBoolean running = new AtomicBoolean(true);
        final ConnectionPool pool = new ConnectionPool(cfg);
        log.info("已建立 {} 个 TCP 连接（复用）", pool.connectionCount());

        // 1. 声明队列
        if (cfg.autoDeclare) {
            declareQueues(pool, cfg, metrics, running);
        }

        // 2. 启动消费者
        final ConsumerManager consumerManager = new ConsumerManager(pool, cfg, metrics, running);
        consumerManager.startConsumers();

        // 3. 启动生产者线程，每个线程负责一批队列
        RateLimiter rateLimiter = new RateLimiter(cfg.producerRate);
        ExecutorService producers = Executors.newFixedThreadPool(cfg.producerThreads);
        List<List<String>> partitions = partition(cfg);
        for (int i = 0; i < cfg.producerThreads; i++) {
            producers.submit(new ProducerWorker(i, partitions.get(i), pool, cfg, metrics, rateLimiter, running));
        }
        log.info("已启动 {} 个生产者线程，共 {} 个队列", cfg.producerThreads, cfg.queueCount);

        // 4. 定时输出指标
        startReporter(cfg, metrics, running);

        // 5. 运行指定时长后停止（0 = 直到 Ctrl+C）
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                running.set(false);
            }
        }));

        if (cfg.durationSeconds > 0) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(cfg.durationSeconds));
            running.set(false);
        } else {
            final CountDownLatch halt = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    halt.countDown();
                }
            }));
            halt.await();
        }

        log.info("压测结束，清理资源...");
        producers.shutdownNow();
        consumerManager.close();
        pool.close();
        printFinalReport(cfg, metrics);
    }

    /** 多线程并发声明队列 */
    private static void declareQueues(final ConnectionPool pool, final PerfConfig cfg,
                                      final Metrics metrics, final AtomicBoolean running) throws InterruptedException {
        log.info("开始声明 {} 个队列（{} 个线程）...", cfg.queueCount, cfg.declareThreads);
        long start = System.currentTimeMillis();
        ExecutorService declarers = Executors.newFixedThreadPool(cfg.declareThreads);
        final int batch = (int) Math.ceil((double) cfg.queueCount / cfg.declareThreads);

        for (int t = 0; t < cfg.declareThreads; t++) {
            final int from = t * batch;
            final int to = Math.min(cfg.queueCount, from + batch);
            declarers.submit(new Runnable() {
                @Override
                public void run() {
                    Channel channel = null;
                    try {
                        channel = pool.createChannel();
                        for (int i = from; i < to && running.get(); i++) {
                            try {
                                // 声明持久队列；routing key 与队列同名，默认交换机即可直达
                                channel.queueDeclare(cfg.queueName(i), cfg.queueDurable, false, false, null);
                                long done = metrics.declared.incrementAndGet();
                                if (done % 1000 == 0) {
                                    log.info("已声明队列 {}/{}", done, cfg.queueCount);
                                }
                            } catch (Exception e) {
                                metrics.declareFailed.incrementAndGet();
                                log.warn("声明队列失败 {}: {}", cfg.queueName(i), e.toString());
                                // channel 可能已关闭，重建
                                closeQuietly(channel);
                                channel = pool.createChannel();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("声明线程异常: {}", e.toString());
                    } finally {
                        closeQuietly(channel);
                    }
                }
            });
        }
        declarers.shutdown();
        declarers.awaitTermination(1, TimeUnit.HOURS);
        log.info("队列声明完成: 成功 {}, 失败 {}, 耗时 {} ms",
                metrics.declared.get(), metrics.declareFailed.get(), System.currentTimeMillis() - start);
    }

    /** 将队列均分给生产者线程 */
    private static List<List<String>> partition(PerfConfig cfg) {
        List<List<String>> partitions = new ArrayList<List<String>>();
        for (int i = 0; i < cfg.producerThreads; i++) {
            partitions.add(new ArrayList<String>());
        }
        for (int i = 0; i < cfg.queueCount; i++) {
            partitions.get(i % cfg.producerThreads).add(cfg.queueName(i));
        }
        return partitions;
    }

    private static void startReporter(final PerfConfig cfg, final Metrics metrics, final AtomicBoolean running) {
        Thread reporter = new Thread(new Runnable() {
            @Override
            public void run() {
                long lastSent = 0, lastRecv = 0;
                while (running.get()) {
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(cfg.reportIntervalSeconds));
                    } catch (InterruptedException e) {
                        return;
                    }
                    long sent = metrics.sent.get();
                    long recv = metrics.received.get();
                    long sendRate = (sent - lastSent) / cfg.reportIntervalSeconds;
                    long recvRate = (recv - lastRecv) / cfg.reportIntervalSeconds;
                    log.info("发送 {} msg/s (累计 {}, 失败 {}) | 消费 {} msg/s (累计 {}, 失败 {}) | 积压 {} | 平均延迟 {} ms, 最大 {} ms",
                            sendRate, sent, metrics.sentFailed.get(),
                            recvRate, recv, metrics.receivedFailed.get(),
                            sent - recv,
                            metrics.avgLatencyMicros() / 1000.0,
                            metrics.latencyMaxMicros.get() / 1000.0);
                    lastSent = sent;
                    lastRecv = recv;
                }
            }
        }, "metrics-reporter");
        reporter.setDaemon(true);
        reporter.start();
    }

    private static void printFinalReport(PerfConfig cfg, Metrics metrics) {
        long sent = metrics.sent.get();
        long recv = metrics.received.get();
        log.info("================ 压测总结 ================");
        log.info("队列数: {}, 连接数: {}", cfg.queueCount, cfg.connectionCount);
        log.info("发送: {} 条 (失败 {})", sent, metrics.sentFailed.get());
        log.info("消费: {} 条 (失败 {})", recv, metrics.receivedFailed.get());
        log.info("积压: {} 条", sent - recv);
        log.info("平均延迟: {} ms, 最大延迟: {} ms",
                metrics.avgLatencyMicros() / 1000.0, metrics.latencyMaxMicros.get() / 1000.0);
    }

    private static void closeQuietly(Channel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception ignored) {
        }
    }
}
