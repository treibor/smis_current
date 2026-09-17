package com.smis.security;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter implements Filter {
    private static final long IDLE_NANOS = Duration.ofMinutes(10).toNanos();
    private static final long CLEANUP_NANOS = Duration.ofMinutes(1).toNanos();
    private final Map<Key, Entry> buckets = new HashMap<>();
    private final LongSupplier ticker;
    private final int capacity;
    private long lastCleanup;

    public RateLimitingFilter() { this(System::nanoTime, 10_000); }

    RateLimitingFilter(LongSupplier ticker, int capacity) {
        this.ticker = ticker;
        this.capacity = capacity;
        this.lastCleanup = ticker.getAsLong();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String route = routeGroup(path);
        if (route == null) {
            chain.doFilter(request, response);
            return;
        }
        // Run after context loading but before login processing. Anonymous clients
        // use the container-resolved IP, never an untrusted forwarding header.
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String client = !authenticated || route.equals("/login")
                ? "ip:" + httpRequest.getRemoteAddr() : "user:" + authentication.getName();
        if (tryConsume(new Key(client, route))) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429);
            httpResponse.setHeader("Retry-After", "60");
            httpResponse.setHeader("Cache-Control", "no-store");
            httpResponse.setContentType("text/plain;charset=UTF-8");
            httpResponse.getWriter().write("Too many requests");
        }
    }

    private synchronized boolean tryConsume(Key key) {
        long now = ticker.getAsLong();
        if (now - lastCleanup >= CLEANUP_NANOS) {
            buckets.values().removeIf(entry -> now - entry.lastSeen >= IDLE_NANOS);
            lastCleanup = now;
        }
        Entry entry = buckets.get(key);
        if (entry == null) {
            // Bound memory without evicting an active client's throttling history.
            if (buckets.size() >= capacity) return false;
            int threshold = switch (key.route()) {
                case "/login" -> 50;
                case "/dashboard" -> 10;
                default -> 100;
            };
            Bucket bucket = Bucket4j.builder().addLimit(Bandwidth.classic(threshold,
                    Refill.greedy(threshold, Duration.ofMinutes(1)))).build();
            entry = new Entry(bucket, now);
            buckets.put(key, entry);
        }
        entry.lastSeen = now;
        return entry.bucket.tryConsume(1);
    }

    private String routeGroup(String path) {
        if (path.equals("/login")) return "/login";
        if (path.equals("/dashboard") || path.endsWith("/dashboard")) return "/dashboard";
        if (path.endsWith("/")) return "/";
        return null;
    }

    private record Key(String client, String route) {}
    private static final class Entry {
        final Bucket bucket;
        long lastSeen;
        Entry(Bucket bucket, long lastSeen) { this.bucket = bucket; this.lastSeen = lastSeen; }
    }
}
