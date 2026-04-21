package com.tech.shortguard.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimiterConfig {
    // Each IP gets its own bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, this::newBucket);
    }

    private Bucket newBucket(String ipAddress) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)                          // 10 requests
                        .refillGreedy(10, Duration.ofMinutes(1)) // per minute
                        .build())
                .build();
    }
}
