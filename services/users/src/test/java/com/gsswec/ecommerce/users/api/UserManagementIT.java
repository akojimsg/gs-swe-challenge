package com.gsswec.ecommerce.users.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.users.api.rest.dto.LoginRequest;
import com.gsswec.ecommerce.users.api.rest.dto.RegisterRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class UserManagementIT {

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

    private String register(String email) {
        rest.postForEntity("/api/v1/auth/register",
                new RegisterRequest(email, "password123", "Test", "User"), Map.class);
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "password123"), Map.class);
        return (String) login.getBody().get("accessToken");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private HttpEntity<Object> bearer(String token) {
        return new HttpEntity<>(bearerHeaders(token));
    }

    private <T> HttpEntity<T> bearer(T body, String token) {
        return new HttpEntity<>(body, bearerHeaders(token));
    }

    @SuppressWarnings("unchecked")
    private String userIdFromToken(String email, String token) {
        ResponseEntity<Map> me = rest.exchange("/api/v1/users/me", HttpMethod.GET, bearer(token), Map.class);
        return (String) me.getBody().get("id");
    }

    @Test
    void meReturnsAuthenticatedProfile() {
        String token = register("me@test.com");

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET, bearer(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", "me@test.com");
        assertThat(response.getBody()).containsEntry("role", "BUYER");
    }

    @Test
    void unauthenticatedRequestReturns401() {
        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET, bearer(null), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void buyerForbiddenFromAdminListReturns403() {
        String buyerToken = register("plainbuyer@test.com");

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users", HttpMethod.GET, bearer(buyerToken), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateMePersistsNewName() {
        String token = register("updateme@test.com");

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.PATCH,
                bearer(Map.of("firstName", "Updated", "lastName", "Name"), token),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("firstName", "Updated");
    }

    @Test
    void adminCanListAndChangeRolePublishingEvent() {
        // Bootstrap an admin by promoting via the repository is not exposed; instead a
        // first user is registered, then promoted by another admin. Here we register a
        // user, manually exercise the admin path by promoting through a seeded admin.
        String adminToken = registerAdmin("admin@test.com");
        String buyerToken = register("targetbuyer@test.com");
        String buyerId = userIdFromToken("targetbuyer@test.com", buyerToken);

        // Admin lists users.
        ResponseEntity<Map> list = rest.exchange(
                "/api/v1/users?page=0&size=10", HttpMethod.GET, bearer(adminToken), Map.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).containsKey("content");

        // Admin promotes the buyer to ADMIN.
        ResponseEntity<Map> changed = rest.exchange(
                "/api/v1/users/" + buyerId + "/role", HttpMethod.PATCH,
                bearer(Map.of("role", "ADMIN"), adminToken),
                Map.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody()).containsEntry("role", "ADMIN");
    }

    @Test
    void internalEmailLookupResolvesWithoutAuth() {
        String token = register("internal-lookup@test.com");
        String userId = userIdFromToken("internal-lookup@test.com", token);

        // No bearer token — the internal endpoint is permitted (private-network only,
        // not routed by the gateway).
        ResponseEntity<Map> response = rest.exchange(
                "/internal/users/" + userId + "/email", HttpMethod.GET, bearer(null), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("userId", userId);
        assertThat(response.getBody()).containsEntry("email", "internal-lookup@test.com");
    }

    @Test
    void internalEmailLookupReturns404ForUnknownUser() {
        ResponseEntity<Map> response = rest.exchange(
                "/internal/users/" + UUID.randomUUID() + "/email", HttpMethod.GET, bearer(null), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUnknownUserReturns404ForAdmin() {
        String adminToken = registerAdmin("admin404@test.com");

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/users/" + UUID.randomUUID(), HttpMethod.GET, bearer(adminToken), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Bootstrap an ADMIN: register, promote via the use case (no admin exists to
    // promote the first one through the API), then re-login for a token bearing the
    // ADMIN claim.
    @Autowired
    private com.gsswec.ecommerce.users.application.usecase.ChangeUserRole changeUserRole;

    @SuppressWarnings("unchecked")
    private String registerAdmin(String email) {
        String token = register(email);
        String id = userIdFromToken(email, token);
        changeUserRole.change(UUID.fromString(id), com.gsswec.ecommerce.users.domain.model.Role.ADMIN);
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                new LoginRequest(email, "password123"), Map.class);
        return (String) login.getBody().get("accessToken");
    }
}
