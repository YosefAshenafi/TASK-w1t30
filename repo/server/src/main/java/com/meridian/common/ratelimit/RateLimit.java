package com.meridian.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the default rate limit for an endpoint.
 * capacity: max tokens in the bucket.
 * refillDurationSeconds: window in seconds for full refill (default 60).
 * type: key source — USER (authenticated user id), IP (remote addr), or USER_AND_IP (both must pass).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int capacity();
    int refillDurationSeconds() default 60;
    RateLimitType type() default RateLimitType.IP;
}
