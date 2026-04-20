package com.meridian.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int defaultCapacity = 120;
    private int defaultRefillSeconds = 60;
    private List<EndpointLimit> limits = new ArrayList<>();

    @Getter
    @Setter
    public static class EndpointLimit {
        private String method;
        private String path;
        private int capacity;
        private int refillSeconds;

        public String key() {
            return method + ":" + path;
        }
    }
}
