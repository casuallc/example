package com.apusic.admq.example.rabbitmq;

import java.util.concurrent.TimeUnit;

/**
 * 简单令牌桶限速器（全局共享，线程安全）。ratePerSecond <= 0 时不限速。
 */
public class RateLimiter {

    private final long ratePerSecond;
    private long availableTokens;
    private long lastRefillNanos;

    public RateLimiter(long ratePerSecond) {
        this.ratePerSecond = ratePerSecond;
        this.availableTokens = ratePerSecond;
        this.lastRefillNanos = System.nanoTime();
    }

    public boolean unlimited() {
        return ratePerSecond <= 0;
    }

    public void acquire() throws InterruptedException {
        if (unlimited()) {
            return;
        }
        synchronized (this) {
            while (true) {
                refill();
                if (availableTokens > 0) {
                    availableTokens--;
                    return;
                }
                long waitNanos = TimeUnit.SECONDS.toNanos(1) / ratePerSecond;
                long waitMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(waitNanos));
                wait(waitMillis);
            }
        }
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        long newTokens = elapsed * ratePerSecond / TimeUnit.SECONDS.toNanos(1);
        if (newTokens > 0) {
            availableTokens = Math.min(ratePerSecond, availableTokens + newTokens);
            lastRefillNanos = now;
        }
    }
}
