package com.meridian;

import com.meridian.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        }
        return ResponseEntity.ok()
                .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                .body(Map.of("status", "UP", "version", "0.0.1-SNAPSHOT"));
    }
}
