package com.gsswec.ecommerce.payments.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsswec.payments")
public record FakePaymentProperties(Double successRate) {

    public FakePaymentProperties {
        successRate = successRate == null ? 0.9 : successRate; // 90% success / 10% failure
    }
}
