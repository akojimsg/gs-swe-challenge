package com.gsswec.ecommerce.products.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
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
class ProductCatalogIT {

    private static final String JWT_SECRET = "products_it_secret_at_least_256_bits_long_xxxxxxx";

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
        registry.add("gsswec.jwt.secret", () -> JWT_SECRET);
    }

    @Autowired
    private TestRestTemplate rest;

    private String adminToken() {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "admin@test.com")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private HttpEntity<Map<String, Object>> adminBody(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> sampleProduct(String name, String sku, double price, int stock, String category) {
        return Map.of("name", name, "sku", sku, "description", name + " description",
                "price", price, "stock", stock, "category", category);
    }

    @SuppressWarnings("unchecked")
    private String createProduct(Map<String, Object> body) {
        ResponseEntity<Map> r = rest.postForEntity("/api/v1/products", adminBody(body), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) r.getBody().get("id");
    }

    @Test
    void publicCanSearchAndAdminCanCreate() {
        createProduct(sampleProduct("Wireless Mouse", "WM-IT1", 29.99, 50, "Electronics"));
        createProduct(sampleProduct("Running Shoes", "RS-IT1", 89.99, 10, "Footwear"));

        // Public full-text search (no auth).
        ResponseEntity<Map> search = rest.getForEntity("/api/v1/products?q=wireless", Map.class);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) search.getBody().get("content");
        assertThat(content).extracting(m -> m.get("sku")).contains("WM-IT1");
        assertThat(content).extracting(m -> m.get("sku")).doesNotContain("RS-IT1");
    }

    @Test
    void priceFilterNarrowsResults() {
        createProduct(sampleProduct("Cheap Thing", "CHEAP-1", 5.00, 100, "Misc"));
        createProduct(sampleProduct("Pricey Thing", "PRICE-1", 500.00, 100, "Misc"));

        ResponseEntity<Map> r = rest.getForEntity("/api/v1/products?minPrice=100", Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) r.getBody().get("content");
        assertThat(content).extracting(m -> m.get("sku")).contains("PRICE-1").doesNotContain("CHEAP-1");
    }

    @Test
    void anonymousCannotCreateProduct() {
        ResponseEntity<Map> r = rest.postForEntity("/api/v1/products",
                new HttpEntity<>(sampleProduct("X", "X-1", 1.0, 1, "Misc")), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateSkuRejectedWith409() {
        createProduct(sampleProduct("First", "DUP-1", 10.0, 5, "Misc"));
        ResponseEntity<Map> r = rest.postForEntity("/api/v1/products",
                adminBody(sampleProduct("Second", "DUP-1", 20.0, 5, "Misc")), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void patchUpdatesAndGetReflectsIt() {
        String id = createProduct(sampleProduct("Patchable", "PATCH-1", 10.0, 5, "Misc"));

        ResponseEntity<Map> patched = rest.exchange("/api/v1/products/" + id, HttpMethod.PATCH,
                adminBody(Map.of("price", 99.99)), Map.class);
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> got = rest.getForEntity("/api/v1/products/" + id, Map.class);
        assertThat(((Number) got.getBody().get("price")).doubleValue()).isEqualTo(99.99);
    }

    @Test
    void deleteRemovesProduct() {
        String id = createProduct(sampleProduct("Deletable", "DEL-1", 10.0, 5, "Misc"));

        ResponseEntity<Void> del = rest.exchange("/api/v1/products/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> got = rest.getForEntity("/api/v1/products/" + id, Map.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void categoriesListIncludesCreatedCategory() {
        createProduct(sampleProduct("Categorized", "CAT-1", 10.0, 5, "UniqueCategoryName"));

        ResponseEntity<List> r = rest.getForEntity("/api/v1/categories", List.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> cats = r.getBody();
        assertThat(cats).extracting(m -> m.get("name")).contains("UniqueCategoryName");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        return headers;
    }
}
