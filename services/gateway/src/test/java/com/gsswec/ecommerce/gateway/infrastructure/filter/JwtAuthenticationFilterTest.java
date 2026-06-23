package com.gsswec.ecommerce.gateway.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.gateway.infrastructure.config.GatewayPolicyProperties;
import com.gsswec.ecommerce.gateway.infrastructure.config.GatewayPolicyProperties.PublicRoute;
import com.gsswec.ecommerce.gateway.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-that-is-at-least-256-bits-long-for-hs256!!";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        var policy = new GatewayPolicyProperties(
                List.of(
                        new PublicRoute("POST", "/api/v1/auth/**"),
                        new PublicRoute("GET", "/api/v1/products"),
                        new PublicRoute("GET", "/api/v1/products/{id}"),
                        new PublicRoute("GET", "/api/v1/categories")),
                null);
        filter = new JwtAuthenticationFilter(new JwtProperties(SECRET), policy);
    }

    private WebFilterChain capturingChain(ServerWebExchange[] sink) {
        return exchange -> {
            sink[0] = exchange;
            return Mono.empty();
        };
    }

    private String token(String sub, String role, Instant exp) {
        return Jwts.builder().subject(sub).claim("role", role)
                .expiration(Date.from(exp)).signWith(key).compact();
    }

    @Test
    void publicGetBypassesAuth() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products"));
        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull(); // not rejected
    }

    @Test
    void protectedRouteWithoutTokenIs401() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"));
        filter.filter(exchange, e -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidTokenIs401() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header("Authorization", "Bearer not.a.jwt"));
        filter.filter(exchange, e -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredTokenIs401() {
        String expired = token("u1", "BUYER", Instant.now().minusSeconds(60));
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header("Authorization", "Bearer " + expired));
        filter.filter(exchange, e -> Mono.empty()).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenForwardsIdentityHeaders() {
        String valid = token("user-123", "ADMIN", Instant.now().plusSeconds(900));
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").header("Authorization", "Bearer " + valid));
        ServerWebExchange[] forwarded = new ServerWebExchange[1];

        filter.filter(exchange, capturingChain(forwarded)).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull(); // not rejected
        var headers = forwarded[0].getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-123");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ADMIN");
    }

    @Test
    void clientSuppliedIdentityHeadersAreOverwritten() {
        String valid = token("real-user", "BUYER", Instant.now().plusSeconds(900));
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer " + valid)
                        .header("X-User-Id", "spoofed")
                        .header("X-User-Role", "ADMIN"));
        ServerWebExchange[] forwarded = new ServerWebExchange[1];

        filter.filter(exchange, capturingChain(forwarded)).block();

        var headers = forwarded[0].getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("real-user");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("BUYER");
    }
}
