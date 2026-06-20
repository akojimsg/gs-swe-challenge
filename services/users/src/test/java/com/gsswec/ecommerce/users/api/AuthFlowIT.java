package com.gsswec.ecommerce.users.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.users.api.rest.dto.LoginRequest;
import com.gsswec.ecommerce.users.api.rest.dto.RegisterRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("gsswec");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.flyway.create-schemas", () -> "true");
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RegisterRequest registration(String email) {
        return new RegisterRequest(email, "password123", "Test", "User");
    }

    @Test
    void registerReturns201WithTokenAndRefreshCookie() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/register", registration("buyer@test.com"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("accessToken");
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) response.getBody().get("user");
        assertThat(user).containsEntry("role", "BUYER");
        assertThat(response.getHeaders().get("Set-Cookie"))
                .anyMatch(c -> c.contains("refresh_token") && c.contains("HttpOnly"));
    }

    @Test
    void registerPublishesUserRegisteredEvent() {
        rest.postForEntity("/api/v1/auth/register", registration("evented@test.com"), Map.class);

        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .range("user.registered", Range.unbounded());

        assertThat(records).isNotNull();
        assertThat(records).anyMatch(r -> "user.registered".equals(r.getValue().get("eventType")));
    }

    @Test
    void duplicateEmailRejectedWith409() {
        rest.postForEntity("/api/v1/auth/register", registration("dupe@test.com"), Map.class);

        ResponseEntity<Map> second = rest.postForEntity(
                "/api/v1/auth/register", registration("dupe@test.com"), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginSucceedsAfterRegistration() {
        rest.postForEntity("/api/v1/auth/register", registration("login@test.com"), Map.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login", new LoginRequest("login@test.com", "password123"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        rest.postForEntity("/api/v1/auth/register", registration("wrongpw@test.com"), Map.class);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login", new LoginRequest("wrongpw@test.com", "nope"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
