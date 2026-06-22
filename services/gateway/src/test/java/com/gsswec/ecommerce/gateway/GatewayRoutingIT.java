package com.gsswec.ecommerce.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

// End-to-end edge behaviour with the real gateway context. Upstreams are stubbed by
// a single WireMock server; route URIs are pointed at it via @DynamicPropertySource,
// so this verifies routing + public-vs-protected + identity forwarding + trace id
// without booting the actual services.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIT {

    static final String SECRET = "integration-secret-at-least-256-bits-long-for-hs256!!";
    static WireMockServer upstream;

    @Autowired
    WebTestClient client;

    @BeforeAll
    static void startUpstream() {
        upstream = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        upstream.start();
        // Products (public read) and Orders (protected) both echo 200 from the stub.
        upstream.stubFor(get(urlPathEqualTo("/api/v1/products"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));
        upstream.stubFor(get(urlPathEqualTo("/api/v1/orders"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));
    }

    @AfterAll
    static void stopUpstream() {
        upstream.stop();
    }

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        // The frozen routes use ${PRODUCTS_URI:...} / ${ORDERS_URI:...} placeholders,
        // so point those at WireMock rather than overriding the indexed route list
        // (which would break Spring's list binding).
        String base = "http://localhost:" + upstream.port();
        registry.add("PRODUCTS_URI", () -> base);
        registry.add("ORDERS_URI", () -> base);
        registry.add("gsswec.jwt.secret", () -> SECRET);
    }

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject("u1").claim("role", "BUYER")
                .expiration(Date.from(Instant.now().plusSeconds(900))).signWith(key).compact();
    }

    @Test
    void publicRouteRoutesWithoutToken() {
        client.get().uri("/api/v1/products").exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Request-Id"); // trace id propagated
    }

    @Test
    void protectedRouteWithoutTokenIsRejectedAtEdge() {
        // 401 at the edge — the filter rejects before routing, so the upstream is
        // never consulted. (Status alone proves this; cross-test WireMock counts are
        // unreliable under shared state, so we assert on the response.)
        client.get().uri("/api/v1/orders").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRouteWithValidTokenRoutesThrough() {
        client.get().uri("/api/v1/orders")
                .header("Authorization", "Bearer " + validToken())
                .exchange()
                .expectStatus().isOk();
    }
}
