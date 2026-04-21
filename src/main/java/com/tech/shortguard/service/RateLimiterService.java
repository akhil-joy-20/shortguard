package com.tech.shortguard.service;

import com.tech.shortguard.config.RateLimiterConfig;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimiterConfig config;

    public boolean isAllowed(String ipAddress) {
        Bucket bucket = config.resolveBucket(ipAddress);
        return bucket.tryConsume(1);
    }

}
