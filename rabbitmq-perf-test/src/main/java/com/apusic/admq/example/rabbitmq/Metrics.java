package com.apusic.admq.example.rabbitmq;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 压测指标统计。
 */
public class Metrics {

    public final AtomicLong sent = new AtomicLong();
    public final AtomicLong sentFailed = new AtomicLong();
    public final AtomicLong received = new AtomicLong();
    public final AtomicLong receivedFailed = new AtomicLong();
    public final AtomicLong declared = new AtomicLong();
    public final AtomicLong declareFailed = new AtomicLong();

    // 延迟统计（微秒累计，配合 received 计算平均延迟）
    public final AtomicLong latencyTotalMicros = new AtomicLong();
    public final AtomicLong latencyMaxMicros = new AtomicLong();

    public void recordLatency(long micros) {
        latencyTotalMicros.addAndGet(micros);
        long cur;
        while (micros > (cur = latencyMaxMicros.get())) {
            if (latencyMaxMicros.compareAndSet(cur, micros)) {
                break;
            }
        }
    }

    public long avgLatencyMicros() {
        long recv = received.get();
        return recv == 0 ? 0 : latencyTotalMicros.get() / recv;
    }
}
