# ShortGuard

A URL Shortener with Redis Caching and Rate Limiting protection built with Spring Boot.

---

## What is ShortGuard?

ShortGuard takes a long URL and converts it into a short, shareable link. It protects against abuse with built-in rate limiting and improves performance with Redis caching.

```
https://www.example.com/very/long/url/here  →  http://localhost:8091/api/v1/aB3xYz
```

---

## Features

- **URL Shortening** — Generates a unique 6-character Base62 short code
- **Redis Caching** — Redirects served from cache in microseconds, no DB hit
- **Rate Limiting** — 10 requests per minute per IP using Bucket4j Token Bucket algorithm
- **Expiry** — URLs expire after a configurable number of days (default 30)
- **Click Tracking** — Tracks how many times each short URL was visited
- **Cache Invalidation** — Deletes Redis key when URL is deleted from DB
- **Input Validation** — Validates URL format and expiry range
- **Swagger UI** — Interactive API documentation

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Database | PostgreSQL |
| Cache | Redis |
| Rate Limiting | Bucket4j |
| Docs | Swagger / SpringDoc OpenAPI |
| Build | Maven |

---

## API Endpoints

### Shorten a URL
```
POST /api/v1/shorten
```
Request:
```json
{
  "url": "https://www.example.com/very/long/url",
  "expiryDays": 30
}
```
Response:
```json
{
  "shortUrl": "http://localhost:8091/api/v1/aB3xYz"
}
```

---

### Redirect to Original URL
```
GET /api/v1/{shortCode}
```
- Returns **302 Found** and redirects to original URL
- Checks Redis first, falls back to DB on cache miss
- Increments click count on every redirect

---

### Get URL Info
```
GET /api/v1/{shortCode}/info
```
Response:
```json
{
  "shortCode": "aB3xYz",
  "longUrl": "https://www.example.com/very/long/url",
  "createdAt": "2024-01-01T10:00:00",
  "expiryAt": "2024-01-31T10:00:00",
  "clickCount": 42
}
```

---

### Delete a URL
```
DELETE /api/v1/{shortCode}
```
- Deletes from DB and Redis cache immediately
- Returns **204 No Content**

---

## How It Works

### URL Shortening Flow
```
POST /shorten
    ↓
Validate input (URL format, expiry range)
    ↓
Check rate limit (10 req/min per IP)
    ↓
Check if URL already exists in DB
    ↓ exists → update expiry → update Redis → return existing shortCode
    ↓ new → generate Base62 shortCode → save to DB → save to Redis → return shortCode
```

### Redirect Flow
```
GET /{shortCode}
    ↓
Check rate limit
    ↓
Check Redis cache
    ↓ hit  → validate expiry from cached value → redirect
    ↓ miss → query DB → validate expiry → store in Redis → redirect
    ↓
Increment click count in DB
```

---

## Rate Limiting

Uses **Bucket4j** with the **Token Bucket** algorithm:

- Each IP address gets its own bucket
- Bucket capacity: **10 tokens**
- Refill: **10 tokens per minute**
- Each request consumes 1 token
- Empty bucket → **429 Too Many Requests**

```json
{
  "error": "Too many requests. Try after 1 minute."
}
```

---

## Redis Caching

- Cache key: `url:{shortCode}`
- Cache value: `{longUrl}||{expiryAt}`
- TTL: 60 minutes
- Expiry stored inside cached value and validated on every cache hit
- Cache deleted on URL deletion

```bash
# Check cached values in Redis CLI
keys *
get url:aB3xYz
ttl url:aB3xYz
```

---

## Setup & Run

### Prerequisites
- Java 21
- PostgreSQL
- Redis

### 1. Clone the repository
```bash
git clone https://github.com/yourname/shortguard.git
cd shortguard
```

### 2. Configure application.properties
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shortguard
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.redis.host=localhost
spring.redis.port=6379

spring.jpa.hibernate.ddl-auto=update
```

### 3. Start Redis
```bash
# Windows via Docker
docker run -d -p 6379:6379 --name redis redis

# Or start downloaded Redis directly
redis-server
```

### 4. Run the application
```bash
mvn spring-boot:run
```

### 5. Open Swagger UI
```
http://localhost:8091/swagger-ui/index.html
```

---

## Design Decisions

| Decision | Reason |
|---|---|
| Base62 encoding | URL-safe characters, 56 billion combinations with 6 chars |
| SecureRandom | Prevents short code enumeration attacks |
| 302 redirect | Preserves rate limiting and click tracking (browser doesn't cache) |
| Redis TTL 60 min | Memory efficient — don't cache for full URL lifetime |
| Expiry in cache value | Keeps Redis TTL short while ensuring expiry accuracy |
| Bucket4j in-memory | No extra infrastructure for single server demo |
| Atomic DB increment | Avoids race condition on click count |

---

## Known Limitations & Future Improvements

- Rate limiting is in-memory — use Redis-backed Bucket4j for distributed/multi-server setup
- No scheduled cleanup job — expired URLs are blocked at access time but remain in DB
- No authentication — anyone can shorten or delete URLs
- No custom alias support — short codes are always random

---

## Swagger

```
http://localhost:8091/swagger-ui/index.html
```

---

## Author

Built as a personal project to study Spring Boot, Redis caching, and rate limiting concepts.
