package com.gsswec.ecommerce.gateway.infrastructure.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public record GatewayPolicyProperties(
        List<PublicRoute> publicRoutes,
        Map<String, RateLimit> rateLimits) {

    public GatewayPolicyProperties {
        publicRoutes = publicRoutes == null ? List.of() : List.copyOf(publicRoutes);
        rateLimits = rateLimits == null ? Map.of() : Map.copyOf(rateLimits);
    }

    public record PublicRoute(String method, String path) {
    }

    public record RateLimit(int limit, String window, String key) {
    }
}
