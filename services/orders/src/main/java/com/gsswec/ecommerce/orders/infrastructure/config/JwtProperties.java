package com.gsswec.ecommerce.orders.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsswec.jwt")
public record JwtProperties(String secret) {
}
