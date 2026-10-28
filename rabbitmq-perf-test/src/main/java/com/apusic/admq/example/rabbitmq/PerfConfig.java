package com.apusic.admq.example.rabbitmq;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 压测配置。默认值 + properties 文件 + 命令行 key=value 覆盖。
 */
public class PerfConfig {

    // ---- 连接与认证 ----
    public String host = "localhost";
    public int port = 5672;
    public String vhost = "/";
    public String username = "guest";
    public String password = "guest";
    /** 复用的 TCP 连接数（所有 channel 在这些连接上多路复用） */
    public int connectionCount = 4;
    public int connectionTimeoutMs = 10000;

    // ---- 队列 ----
    public int queueCount = 1000;
    public String queuePrefix = "perf-queue-";
    public boolean queueDurable = true;
    /** 启动时是否声明队列 */
    public boolean autoDeclare = true;
    /** 声明队列的并发线程数 */
    public int declareThreads = 8;

    // ---- 生产者 ----
    public int producerThreads = 8;
    public int messageSize = 1024;
    /** 全局发送速率（条/秒），0 表示不限速 */
    public long producerRate = 0;
    /** 消息是否持久化 */
    public boolean persistent = false;
    /** 消息体前 8 字节写入发送时间戳，用于消费端计算延迟 */
    public boolean measureLatency = true;

    // ---- 消费者 ----
    public int consumerPrefetch = 100;
    public boolean consumerAutoAck = true;
    /** 每个 channel 上挂多少个消费者（消费者:channel = consumersPerChannel:1） */
    public int consumersPerChannel = 100;

    // ---- 运行 ----
    /** 压测时长（秒），0 表示一直运行直到手动停止 */
    public long durationSeconds = 0;
    public int reportIntervalSeconds = 5;

    public static PerfConfig load(String[] args) throws IOException {
        PerfConfig cfg = new PerfConfig();

        String file = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                file = arg.substring("--config=".length());
            }
        }
        Properties props = new Properties();
        if (file != null) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            cfg.apply(props);
        }

        // 命令行 key=value 覆盖（key 不带前缀，例如 host=192.168.1.10 queueCount=5000）
        Properties cli = new Properties();
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                continue;
            }
            int idx = arg.indexOf('=');
            if (idx > 0) {
                cli.setProperty(arg.substring(0, idx), arg.substring(idx + 1));
            }
        }
        cfg.apply(cli);
        return cfg;
    }

    private void apply(Properties p) {
        host = get(p, "host", host);
        port = getInt(p, "port", port);
        vhost = get(p, "vhost", vhost);
        username = get(p, "username", username);
        password = get(p, "password", password);
        connectionCount = getInt(p, "connectionCount", connectionCount);
        connectionTimeoutMs = getInt(p, "connectionTimeoutMs", connectionTimeoutMs);

        queueCount = getInt(p, "queueCount", queueCount);
        queuePrefix = get(p, "queuePrefix", queuePrefix);
        queueDurable = getBool(p, "queueDurable", queueDurable);
        autoDeclare = getBool(p, "autoDeclare", autoDeclare);
        declareThreads = getInt(p, "declareThreads", declareThreads);

        producerThreads = getInt(p, "producerThreads", producerThreads);
        messageSize = getInt(p, "messageSize", messageSize);
        producerRate = getLong(p, "producerRate", producerRate);
        persistent = getBool(p, "persistent", persistent);
        measureLatency = getBool(p, "measureLatency", measureLatency);

        consumerPrefetch = getInt(p, "consumerPrefetch", consumerPrefetch);
        consumerAutoAck = getBool(p, "consumerAutoAck", consumerAutoAck);
        consumersPerChannel = getInt(p, "consumersPerChannel", consumersPerChannel);

        durationSeconds = getLong(p, "durationSeconds", durationSeconds);
        reportIntervalSeconds = getInt(p, "reportIntervalSeconds", reportIntervalSeconds);
    }

    public String queueName(int index) {
        return queuePrefix + index;
    }

    private static String get(Properties p, String key, String def) {
        String v = p.getProperty(key);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private static int getInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        return v == null ? def : Integer.parseInt(v.trim());
    }

    private static long getLong(Properties p, String key, long def) {
        String v = p.getProperty(key);
        return v == null ? def : Long.parseLong(v.trim());
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    @Override
    public String toString() {
        return "PerfConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", vhost='" + vhost + '\'' +
                ", username='" + username + '\'' +
                ", connectionCount=" + connectionCount +
                ", queueCount=" + queueCount +
                ", queuePrefix='" + queuePrefix + '\'' +
                ", queueDurable=" + queueDurable +
                ", autoDeclare=" + autoDeclare +
                ", producerThreads=" + producerThreads +
                ", messageSize=" + messageSize +
                ", producerRate=" + producerRate +
                ", persistent=" + persistent +
                ", consumerPrefetch=" + consumerPrefetch +
                ", consumerAutoAck=" + consumerAutoAck +
                ", consumersPerChannel=" + consumersPerChannel +
                ", durationSeconds=" + durationSeconds +
                '}';
    }
}
