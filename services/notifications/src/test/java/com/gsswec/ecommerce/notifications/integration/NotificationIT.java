package com.gsswec.ecommerce.notifications.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.notifications.infrastructure.persistence.NotificationLogJpaRepository;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.user.UserRegisteredEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class NotificationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gsswec")
            .withUsername("gsswec")
            .withPassword("change_me_local_dev");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
        registry.add("gsswec.jwt.secret", () -> "change_me_base64_256_bit_secret_change_me_please");
    }

    @Autowired StringRedisTemplate redisTemplate;
    @Autowired NotificationLogJpaRepository jpaRepository;
    @Autowired ObjectMapper objectMapper;

    // Publish an event in the real {eventId, eventType, payload} envelope every
    // service uses, with the event record serialized into payload.
    private void publish(String stream, UUID eventId, Object event) throws Exception {
        redisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(stream)
                .ofMap(Map.of(
                        "eventId", eventId.toString(),
                        "eventType", stream,
                        "payload", objectMapper.writeValueAsString(event))));
    }

    private UserRegisteredEvent userRegistered(UUID eventId, String email) {
        return new UserRegisteredEvent(
                new BaseEvent(eventId, StreamNames.USER_REGISTERED, "1.0", Instant.now(), null, "users"),
                UUID.randomUUID(), email, "BUYER");
    }

    @Test
    void userRegisteredEventProducesLogEntry() throws Exception {
        UUID eventId = UUID.randomUUID();

        publish(StreamNames.USER_REGISTERED, eventId, userRegistered(eventId, "newuser@example.com"));

        await().atMost(Duration.ofSeconds(15))
                .until(() -> jpaRepository.existsByEventId(eventId));

        var entry = jpaRepository.findAll().stream()
                .filter(e -> e.getEventId().equals(eventId))
                .findFirst().orElseThrow();
        // SMTP may refuse (no Mailhog in test), but the log must be persisted either way,
        // and the recipient must be the address from the payload — proving deserialization.
        assertTrue(entry.getStatus().equals("SENT") || entry.getStatus().equals("FAILED"));
        assertEquals(StreamNames.USER_REGISTERED, entry.getEventType());
        assertEquals("newuser@example.com", entry.getRecipient());
    }

    @Test
    void duplicateEventIdIsIdempotent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UserRegisteredEvent event = userRegistered(eventId, "dupe@example.com");

        // Same eventId delivered twice (at-least-once) must yield exactly one log row.
        publish(StreamNames.USER_REGISTERED, eventId, event);
        publish(StreamNames.USER_REGISTERED, eventId, event);

        await().atMost(Duration.ofSeconds(15))
                .until(() -> jpaRepository.existsByEventId(eventId));

        long count = jpaRepository.findAll().stream()
                .filter(e -> e.getEventId().equals(eventId))
                .count();
        assertEquals(1, count, "Duplicate event must only produce one log entry");
    }
}
