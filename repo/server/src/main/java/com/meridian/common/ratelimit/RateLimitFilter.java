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
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(15)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_BUCKETS = 50_000;

    @SuppressWarnings("unchecked")
    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<String, Bucket>(MAX_BUCKETS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_BUCKETS;
                }
            });

    private final ObjectMapper objectMapper;
    private final RateLimitProperties props;
    private final Map<String, BandwidthConfig> endpointLimits;

    public RateLimitFilter(ObjectMapper objectMapper, RateLimitProperties props) {
        this.objectMapper = objectMapper;
        this.props = props;
        this.endpointLimits = props.getLimits().stream()
                .collect(Collectors.toMap(
                        RateLimitProperties.EndpointLimit::key,
                        l -> new BandwidthConfig(l.getCapacity(), l.getRefillSeconds())));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!props.isEnabled()) {
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
        if (endpointLimits.containsKey(endpointKey)) {
            return endpointKey + ":" + userOrIp;
        }
        return "default:" + userOrIp;
    }

    private BandwidthConfig resolveConfig(HttpServletRequest request) {
        String endpointKey = request.getMethod() + ":" + request.getRequestURI();
        return endpointLimits.getOrDefault(endpointKey,
                new BandwidthConfig(props.getDefaultCapacity(), props.getDefaultRefillSeconds()));
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
