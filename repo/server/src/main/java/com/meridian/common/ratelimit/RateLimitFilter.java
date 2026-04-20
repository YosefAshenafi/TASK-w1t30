package com.meridian.common.ratelimit;

import com.meridian.common.web.ErrorEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Order(15)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int DEFAULT_CAPACITY = 120;
    private static final int DEFAULT_REFILL_SECONDS = 60;

    private static final Map<String, BandwidthConfig> ENDPOINT_LIMITS = Map.of(
            "POST:/api/v1/auth/login", new BandwidthConfig(10, 60),
            "POST:/api/v1/auth/register", new BandwidthConfig(5, 60),
            "POST:/api/v1/reports", new BandwidthConfig(30, 60),
            "POST:/api/v1/reports/schedules", new BandwidthConfig(30, 60),
            "POST:/api/v1/sessions/sync", new BandwidthConfig(60, 60)
    );

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        String bucketKey = buildBucketKey(request);
        BandwidthConfig config = resolveConfig(request);
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> buildBucket(config));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = (probe.getNanosToWaitForRefill() / 1_000_000_000L) + 1;
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(objectMapper.writeValueAsString(
                    ErrorEnvelope.of("RATE_LIMITED",
                            "Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds.")));
            return;
        }
        chain.doFilter(request, response);
    }

    private String buildBucketKey(HttpServletRequest request) {
        String endpointKey = request.getMethod() + ":" + request.getRequestURI();
        String userOrIp = resolveUserOrIpKey(request);
        if (ENDPOINT_LIMITS.containsKey(endpointKey)) {
            return endpointKey + ":" + userOrIp;
        }
        return "default:" + userOrIp;
    }

    private BandwidthConfig resolveConfig(HttpServletRequest request) {
        String endpointKey = request.getMethod() + ":" + request.getRequestURI();
        return ENDPOINT_LIMITS.getOrDefault(endpointKey,
                new BandwidthConfig(DEFAULT_CAPACITY, DEFAULT_REFILL_SECONDS));
    }

    private String resolveUserOrIpKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }
        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String addr) {
        return addr != null && (addr.startsWith("127.") || addr.startsWith("10.")
                || addr.startsWith("192.168.") || addr.equals("::1")
                || (addr.startsWith("172.") && isTrustedPrivate172(addr)));
    }

    private boolean isTrustedPrivate172(String addr) {
        try {
            int second = Integer.parseInt(addr.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) { return false; }
    }

    private Bucket buildBucket(BandwidthConfig config) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.capacity())
                .refillGreedy(config.capacity(), Duration.ofSeconds(config.refillDurationSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    record BandwidthConfig(int capacity, int refillDurationSeconds) {}
}
