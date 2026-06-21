package com.gsswec.ecommerce.payments.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.payments.application.port.out.EventPublisher;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.Map;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamEventPublisher implements EventPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisStreamEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String stream, DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redis.opsForStream().add(StreamRecords.newRecord()
                    .in(stream)
                    .ofMap(Map.of(
                            "eventId", event.base().eventId().toString(),
                            "eventType", event.base().eventType(),
                            "payload", payload)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event " + event.base().eventType(), e);
        }
    }
}
