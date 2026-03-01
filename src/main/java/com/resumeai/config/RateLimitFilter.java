package com.resumeai.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        // Allow 5 requests per minute
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String clientIp) {
        return cache.computeIfAbsent(clientIp, k -> createNewBucket());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Exclude polling endpoints from rate limiting
        if (path.endsWith("/status") || path.endsWith("/matches")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Only rate limit AI-heavy endpoints
        if (path.contains("/score") || path.contains("/analyze-compatibility") ||
            path.contains("/tailor") || path.contains("/find-candidates") || path.contains("/gap-analysis")) {

            // In a real prod app, use the authenticated user ID. Using IP for simplicity if no user is found
            String key = request.getRemoteAddr();
            if (request.getUserPrincipal() != null) {
                key = request.getUserPrincipal().getName();
            }

            Bucket bucket = resolveBucket(key);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests - Rate limit exceeded");
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
