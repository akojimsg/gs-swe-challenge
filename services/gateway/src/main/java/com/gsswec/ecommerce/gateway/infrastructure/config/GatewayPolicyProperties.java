package com.gsswec.ecommerce.gateway.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the FROZEN `gateway:` policy block in application.yml so the edge filters
// read the contract rather than hardcoding it.
//
// - publicRoutes: method+path entries that bypass JWT (everything else requires a
//   valid token).
// - rateLimits: per-route limit/window/key table (consumed by the rate-limit filter
//   in the follow-up PR; bound here so the contract has one home).
@ConfigurationProperties(prefix = "gateway")
public record GatewayPolicyProperties(
        List<PublicRoute> publicRoutes,
        java.util.Map<String, RateLimit> rateLimits) {

    public GatewayPolicyProperties {
        publicRoutes = publicRoutes == null ? List.of() : List.copyOf(publicRoutes);
        rateLimits = rateLimits == null ? java.util.Map.of() : java.util.Map.copyOf(rateLimits);
    }

    // A path that bypasses authentication for the given HTTP method.
    public record PublicRoute(String method, String path) {
    }

    // limit requests per window, keyed by IP or userId. window is an ISO-ish
    // shorthand ("1m", "1h"); parsed by the rate-limit filter when it lands.
    public record RateLimit(int limit, String window, String key) {
    }
}
