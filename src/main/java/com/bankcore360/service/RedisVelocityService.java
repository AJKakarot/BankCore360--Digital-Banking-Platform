package com.bankcore360.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Service
public class RedisVelocityService {

    private static final Logger log = LoggerFactory.getLogger(RedisVelocityService.class);

    private final StringRedisTemplate redisTemplate;

    // Local in-memory sliding-window fallback if Redis cluster is temporarily unreachable
    private final Map<String, ConcurrentLinkedQueue<WindowEntry>> localFallbackStore = new ConcurrentHashMap<>();

    private record WindowEntry(long timestamp, BigDecimal amount, String ref) {}

    @Autowired
    public RedisVelocityService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Records a financial transaction event into the sliding velocity buffer.
     */
    public void recordTransaction(String accountNumber, BigDecimal amount, String transactionReference) {
        long now = Instant.now().toEpochMilli();
        String member = transactionReference + ":" + amount.toPlainString() + ":" + now;
        String key = "fraud:velocity:ts:" + accountNumber;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForZSet().add(key, member, (double) now);
                redisTemplate.expire(key, 1, TimeUnit.HOURS);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for recordTransaction; falling back to local memory buffer: {}", e.getMessage());
        }

        // Fallback local memory
        localFallbackStore.computeIfAbsent(accountNumber, k -> new ConcurrentLinkedQueue<>())
                .add(new WindowEntry(now, amount, transactionReference));
    }

    /**
     * Calculates the number of transactions conducted by the account within the specified seconds.
     */
    public int getTransactionCountInWindow(String accountNumber, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);
        String key = "fraud:velocity:ts:" + accountNumber;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
                // Prune expired entries outside window
                zset.removeRangeByScore(key, 0, (double) (windowStart - 1));
                Long count = zset.count(key, (double) windowStart, (double) now);
                return (count != null) ? count.intValue() : 0;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for getTransactionCountInWindow; reading local memory buffer: {}", e.getMessage());
        }

        // Local fallback
        ConcurrentLinkedQueue<WindowEntry> queue = localFallbackStore.get(accountNumber);
        if (queue == null) return 0;
        queue.removeIf(entry -> entry.timestamp < windowStart);
        return queue.size();
    }

    /**
     * Detects rapid succession spikes (e.g., more than N attempts within 15 seconds).
     */
    public boolean isRapidBurst(String accountNumber, int burstWindowSeconds, int maxBurstAllowed) {
        int count = getTransactionCountInWindow(accountNumber, burstWindowSeconds);
        return count >= maxBurstAllowed;
    }

    /**
     * Calculates total volume transferred by this account in the given sliding window.
     */
    public BigDecimal getVolumeInWindow(String accountNumber, int windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);
        String key = "fraud:velocity:ts:" + accountNumber;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                var entries = redisTemplate.opsForZSet().rangeByScore(key, (double) windowStart, (double) now);
                if (entries != null) {
                    BigDecimal total = BigDecimal.ZERO;
                    for (String item : entries) {
                        String[] parts = item.split(":");
                        if (parts.length >= 2) {
                            total = total.add(new BigDecimal(parts[1]));
                        }
                    }
                    return total;
                }
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for getVolumeInWindow; calculating from local memory buffer: {}", e.getMessage());
        }

        ConcurrentLinkedQueue<WindowEntry> queue = localFallbackStore.get(accountNumber);
        if (queue == null) return BigDecimal.ZERO;
        return queue.stream()
                .filter(e -> e.timestamp >= windowStart)
                .map(WindowEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

