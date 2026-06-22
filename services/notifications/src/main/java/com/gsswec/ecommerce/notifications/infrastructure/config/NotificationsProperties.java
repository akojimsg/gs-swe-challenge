package com.gsswec.ecommerce.notifications.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsswec.notifications")
public record NotificationsProperties(
        String fromAddress,
        String adminEmail,
        String usersBaseUrl,
        Integer userCacheTtlMinutes) {

    public NotificationsProperties {
        fromAddress = fromAddress == null ? "noreply@gsswec.com" : fromAddress;
        adminEmail = adminEmail == null ? "admin@gsswec.com" : adminEmail;
        usersBaseUrl = usersBaseUrl == null ? "http://users:8081" : usersBaseUrl;
        userCacheTtlMinutes = userCacheTtlMinutes == null ? 30 : userCacheTtlMinutes;
    }
}
