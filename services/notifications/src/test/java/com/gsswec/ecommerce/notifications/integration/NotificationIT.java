package com.gsswec.ecommerce.notifications.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gsswec.ecommerce.notifications.infrastructure.persistence.NotificationLogJpaRepository;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        registry.add("gsswec.notifications.from-address", () -> "noreply@gsswec.com");
    }

    @Autowired StringRedisTemplate redisTemplate;
    @Autowired NotificationLogJpaRepository jpaRepository;

    @Test
    void userRegisteredEventProducesLogEntry() {
        UUID eventId = UUID.randomUUID();

        redisTemplate.opsForStream().add(StreamNames.USER_REGISTERED, Map.of(
                "eventId", eventId.toString(),
                "email", "newuser@example.com",
                "name", "Test User"));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> jpaRepository.existsByEventId(eventId));

        var log = jpaRepository.findAll().stream()
                .filter(e -> e.getEventId().equals(eventId))
                .findFirst().orElseThrow();
        // SMTP may refuse (no Mailhog in test), but the log must be persisted either way
        assertTrue(log.getStatus().equals("SENT") || log.getStatus().equals("FAILED"));
        assertEquals(StreamNames.USER_REGISTERED, log.getEventType());
    }

    @Test
    void duplicateEventIdIsIdempotent() {
        UUID eventId = UUID.randomUUID();
        Map<String, String> fields = Map.of(
                "eventId", eventId.toString(),
                "email", "buyer@example.com",
                "orderId", UUID.randomUUID().toString());

        redisTemplate.opsForStream().add(StreamNames.ORDER_PAID, fields);
        redisTemplate.opsForStream().add(StreamNames.ORDER_PAID, fields);

        await().atMost(Duration.ofSeconds(10))
                .until(() -> jpaRepository.existsByEventId(eventId));

        long count = jpaRepository.findAll().stream()
                .filter(e -> e.getEventId().equals(eventId))
                .count();
        assertEquals(1, count, "Duplicate event must only produce one log entry");
    }
}
