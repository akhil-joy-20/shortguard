package com.tech.shortguard.controller;

import com.tech.shortguard.dto.ShortenRequest;
import com.tech.shortguard.entity.UrlMapping;
import com.tech.shortguard.exception.UrlNotFoundException;
import com.tech.shortguard.repository.UrlMappingRepository;
import com.tech.shortguard.service.RateLimiterService;
import com.tech.shortguard.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "UrlShortener", description = "URL Shortener APIs")
public class UrlShortenerController {

    private final UrlShortenerService service;
    private final RateLimiterService rateLimiterService;
    private final UrlMappingRepository repository;

    @Operation(summary = "Shorten a URL")
    @PostMapping("/shorten")
    public ResponseEntity<?> shorten(@Valid @RequestBody ShortenRequest request,
                                     HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();

        if (!rateLimiterService.isAllowed(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Try after 1 minute."));
        }

        String shortCode = service.shortenUrl(request.getUrl(), request.getExpiryDays());

        return ResponseEntity.ok(Map.of("shortUrl", "http://localhost:8091/api/v1/" + shortCode));
    }
//    {
//        "url": "https://start.spring.io/",
//            "expiryDays": "2"
//    }


    @Operation(summary = "Redirect to original URL")
    @GetMapping("/{shortCode}") //http://localhost:8091/api/v1/{shortCode} in browser/postman - swagger may not work
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                         HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();

        if (!rateLimiterService.isAllowed(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        String longUrl = service.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND) // 302
                .location(URI.create(longUrl))
                .build();
    }

    @Operation(summary = "Delete the url from DB and Cache")
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortCode) {
        service.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get URL info with click count")
    @GetMapping("/{shortCode}/info")
    public ResponseEntity<?> getInfo(@PathVariable String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        return ResponseEntity.ok(Map.of(
                "shortCode", mapping.getShortCode(),
                "longUrl", mapping.getLongUrl(),
                "createdAt", mapping.getCreatedAt(),
                "expiryAt", mapping.getExpiryAt(),
                "clickCount", mapping.getClickCount()
        ));
    }
}
