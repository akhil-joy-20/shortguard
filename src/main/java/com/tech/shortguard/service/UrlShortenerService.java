package com.tech.shortguard.service;

import com.tech.shortguard.entity.UrlMapping;
import com.tech.shortguard.exception.UrlNotFoundException;
import com.tech.shortguard.repository.UrlMappingRepository;
import com.tech.shortguard.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "url:";
    private static final long CACHE_TTL_MINUTES = 60; // TTL (Time To Live)


    public String shortenUrl(String longUrl, int expiryDays) {

        Optional<UrlMapping> existing = repository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            UrlMapping existingMapping  = existing.get();

            // Reset expiry from today
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(expiryDays);
            existingMapping.setExpiryAt(newExpiry);
            repository.save(existingMapping);

            // Update Redis too
            String valueToCache = existingMapping.getLongUrl() + "||" + newExpiry.toString();
            redisTemplate.opsForValue().set(
                    CACHE_PREFIX + existingMapping.getShortCode(),
                    valueToCache,
                    CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );

            return existingMapping.getShortCode();
        }

        String shortCode;
        do {
            shortCode = ShortCodeGenerator.generate();
        } while (repository.existsByShortCode(shortCode));

        LocalDateTime expiryAt = LocalDateTime.now().plusDays(expiryDays);

        UrlMapping mapping = UrlMapping.builder()
                .longUrl(longUrl)
                .shortCode(shortCode)
                .createdAt(LocalDateTime.now())
                .expiryAt(expiryAt)
                .build();

        repository.save(mapping);

        // Store in Redis after saving
        // Store URL + expiry together (same format as getLongUrl)
        String valueToCache = longUrl + "||" + expiryAt.toString();
        redisTemplate.opsForValue().set(
                CACHE_PREFIX + shortCode,
                valueToCache,
                CACHE_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return shortCode;
    }

    public String getLongUrl(String shortCode) {

        // 1. Check Redis first
        String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
        if (cached != null) {
            // Split URL and expiry
            String[] parts = cached.split("\\|\\|");
            String longUrl = parts[0];
            LocalDateTime expiry = LocalDateTime.parse(parts[1]);

            // Check expiry even on cache hit
            if (expiry.isBefore(LocalDateTime.now())) {
                redisTemplate.delete(CACHE_PREFIX + shortCode);
                throw new UrlNotFoundException("URL expired");
            }
            log.info("Cache hit for: {}", shortCode);
            incrementClickCount(shortCode);
            return longUrl;
        }

        // 2. If not in Redis, go to DB
        log.info("Cache miss for: {}", shortCode);
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        if (mapping.getExpiryAt() != null &&
                mapping.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException("URL expired");
        }

        // 3. Store in Redis for next time
        String valueToCache = mapping.getLongUrl() + "||" + mapping.getExpiryAt().toString();
        redisTemplate.opsForValue().set(
                CACHE_PREFIX + shortCode,
                valueToCache,
                CACHE_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        incrementClickCount(shortCode);
        return mapping.getLongUrl();
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        // Delete from DB
        repository.deleteByShortCode(shortCode);

        // Delete from Redis immediately
        redisTemplate.delete(CACHE_PREFIX + shortCode);

        log.info("Deleted URL and cache for: {}", shortCode);
    }

    @Transactional
    public void incrementClickCount(String shortCode) {
        repository.incrementClickCount(shortCode);
    }
}
