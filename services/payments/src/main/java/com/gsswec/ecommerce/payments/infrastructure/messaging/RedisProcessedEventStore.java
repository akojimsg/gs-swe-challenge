package com.gsswec.ecommerce.payments.infrastructure.messaging;

import com.gsswec.ecommerce.payments.application.port.out.ProcessedEventStore;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// SETNX-backed dedupe ledger. The key TTL bounds storage growth — far longer than
// any redelivery window, short enough to not accumulate forever.
@Component
public class RedisProcessedEventStore implements ProcessedEventStore {

    private static final String KEY_PREFIX = "payments:processed:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    public RedisProcessedEventStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean markIfNew(UUID eventId) {
        Boolean wasAbsent = redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        return Boolean.TRUE.equals(wasAbsent);
    }
}
