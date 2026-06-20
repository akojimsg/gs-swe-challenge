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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    // --- session lifecycle (#5) ----------------------------------------------

    private String registerAndGetRefreshCookie(String email) {
        ResponseEntity<Map> reg = rest.postForEntity(
                "/api/v1/auth/register", registration(email), Map.class);
        return reg.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst().orElseThrow();
    }

    private ResponseEntity<Map> postWithCookie(String path, String cookie, Class<Map> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), type);
    }

    @Test
    void refreshRotatesAndReturnsNewAccessToken() {
        String cookie = registerAndGetRefreshCookie("refresh@test.com");

        ResponseEntity<Map> response = postWithCookie("/api/v1/auth/refresh", cookie, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anyMatch(c -> c.startsWith("refresh_token=") && !c.startsWith("refresh_token=;"));
    }

    @Test
    void reusedOldRefreshTokenIsRejectedAfterRotation() {
        String cookie = registerAndGetRefreshCookie("reuse@test.com");

        // First rotation succeeds and revokes the original token.
        assertThat(postWithCookie("/api/v1/auth/refresh", cookie, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Replaying the now-revoked original token must fail.
        assertThat(postWithCookie("/api/v1/auth/refresh", cookie, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshWithoutCookieReturns401() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/refresh", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTokenAndReturns204() {
        String cookie = registerAndGetRefreshCookie("logout@test.com");

        ResponseEntity<Map> logout = postWithCookie("/api/v1/auth/logout", cookie, Map.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // The revoked token can no longer be refreshed.
        assertThat(postWithCookie("/api/v1/auth/refresh", cookie, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutWithoutCookieIsIdempotent204() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/logout", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
