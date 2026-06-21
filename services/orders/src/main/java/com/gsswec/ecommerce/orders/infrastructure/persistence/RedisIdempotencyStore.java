package com.gsswec.ecommerce.orders.infrastructure.persistence;

import com.gsswec.ecommerce.orders.application.port.out.IdempotencyStore;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "orders:idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public RedisIdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<UUID> get(String idempotencyKey) {
        String value = redis.opsForValue().get(KEY_PREFIX + idempotencyKey);
        return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
    }

    @Override
    public void put(String idempotencyKey, UUID orderId) {
        redis.opsForValue().set(KEY_PREFIX + idempotencyKey, orderId.toString(), TTL);
    }
}
