package com.meridian.common.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Reads the Idempotency-Key header and stores it as a request attribute.
 * Full idempotency enforcement is implemented in Phase 8 (IdempotencyService).
 */
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_KEY_ATTR = "idempotencyKey";

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) {
        String key = request.getHeader("Idempotency-Key");
        if (key != null && !key.isBlank()) {
            request.setAttribute(IDEMPOTENCY_KEY_ATTR, key);
        }
        return true;
    }
}
