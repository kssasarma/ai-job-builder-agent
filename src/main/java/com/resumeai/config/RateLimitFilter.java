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

    private Bucket createBucket(int limitReq) {
        Bandwidth limit = Bandwidth.classic(limitReq, Refill.greedy(limitReq, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String key, int limitReq) {
        return cache.computeIfAbsent(key, k -> createBucket(limitReq));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        if (path.endsWith("/status") || path.endsWith("/matches")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        int limit = 0;
        String type = "";
        if (path.contains("/score")) { limit = 10; type = "scoring"; }
        else if (path.contains("/find-candidates")) { limit = 5; type = "matching"; }
        else if (path.contains("/tailor")) { limit = 10; type = "tailoring"; }
        else if (path.contains("/analyze-compatibility") || path.contains("/gap-analysis")) { limit = 10; type = "analysis"; }

        if (limit > 0) {
            String userId = request.getRemoteAddr();
            if (request.getUserPrincipal() != null) {
                userId = request.getUserPrincipal().getName();
            }

            String key = userId + "_" + type;
            Bucket bucket = resolveBucket(key, limit);

            // Allow total max 20 per minute across AI endpoints
            String globalKey = userId + "_global";
            Bucket globalBucket = resolveBucket(globalKey, 20);

            if (globalBucket.tryConsume(1) && bucket.tryConsume(1)) {
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
