package com.gsswec.ecommerce.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Shared HS256 signing secret. The gateway only VERIFIES tokens (Users mints them);
// same property key and secret as the services so the whole platform validates
// against one key. See services/*/infrastructure/config/JwtProperties.
@ConfigurationProperties(prefix = "gsswec.jwt")
public record JwtProperties(String secret) {
}
