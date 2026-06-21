package com.gsswec.ecommerce.orders.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.Map;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RedisStreamEventPublisher implements EventPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisStreamEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // Publish AFTER the surrounding transaction commits. Serialize eagerly (so a bad
    // payload fails the business tx, not a post-commit callback), but defer the actual
    // stream write to afterCommit. This removes the dual-write race: a consumer can
    // never observe order.placed before the order row is durably committed, and a
    // rolled-back tx publishes nothing. With no active transaction (tests, non-tx
    // callers) we publish immediately.
    @Override
    public void publish(String stream, DomainEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event " + event.base().eventType(), e);
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write(stream, event, payload);
                }
            });
        } else {
            write(stream, event, payload);
        }
    }

    private void write(String stream, DomainEvent event, String payload) {
        redis.opsForStream().add(StreamRecords.newRecord()
                .in(stream)
                .ofMap(Map.of(
                        "eventId", event.base().eventId().toString(),
                        "eventType", event.base().eventType(),
                        "payload", payload)));
    }
}
