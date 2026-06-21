package com.gsswec.ecommerce.notifications.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsswec.notifications")
public record NotificationsProperties(String fromAddress) {}
