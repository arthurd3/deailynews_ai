package com.arthur.newsbrief.shared.web;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps how many requests a single client may make.
 *
 * <p>{@code @ConcurrencyLimit} on the service stops parallel inference, but it does nothing
 * about one client issuing a hundred requests in sequence. Vary the country and category and
 * each of those misses the cache and costs real inference time, on a public endpoint.
 *
 * <p>Buckets are held in a Caffeine cache keyed by client, expiring on idle, so a burst of
 * distinct callers cannot grow the map without bound — a plain {@code ConcurrentHashMap} here
 * would be a slow memory leak.
 *
 * <p>Resilience4j is already on the classpath but its rate limiter is per-instance, not
 * per-client, which is the wrong shape for this.
 */
class ClientRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ClientRateLimitFilter.class);

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;

    ClientRateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(properties.period().multipliedBy(10).toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(10_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : properties.paths()) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Bucket bucket = buckets.get(clientKey(request), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        log.debug("Rate limit hit; asking the client to retry in {}s", retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"urn:newsbrief:problem:rate-limited",\
                "title":"Too many requests",\
                "status":429,\
                "detail":"You have used this endpoint's budget. Retry in %d seconds."}"""
                .formatted(retryAfterSeconds));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(properties.capacity())
                        .refillGreedy(properties.capacity(), properties.period()))
                .build();
    }

    /**
     * Identifies the caller. X-Forwarded-For is honoured because the app is expected to sit
     * behind a proxy in any real deployment; the first entry is the original client.
     */
    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
