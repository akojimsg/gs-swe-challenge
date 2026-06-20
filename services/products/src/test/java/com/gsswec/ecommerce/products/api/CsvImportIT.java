package com.gsswec.ecommerce.products.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.awaitility.Awaitility;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CsvImportIT {

    private static final String JWT_SECRET = "import_it_secret_at_least_256_bits_long_xxxxxxxxx";

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
        return Jwts.builder().subject(UUID.randomUUID().toString()).claim("role", "ADMIN")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    private static final String CSV = """
            name,sku,description,category,price,stock,weight_kg
            Good One,GOOD-1,desc,Electronics,29.99,50,0.1
            Free Price,FREE-1,bad price,Sports,free,200,1.2
            Negative Stock,NEG-1,bad stock,Home,45.50,-5,2.1
            ,NONAME-1,missing name,Misc,5.00,10,0.1
            Good Two,GOOD-2,desc,Footwear,$89.99,10,0.35
            Good One,GOOD-1,updated same sku,Electronics,34.99,40,0.1

            """;

    @SuppressWarnings("unchecked")
    @Test
    void importReports202ThenPartialSuccessSummary() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(CSV.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "catalog.csv";
            }
        });

        ResponseEntity<Map> accepted = rest.postForEntity(
                "/api/v1/products/import", new HttpEntity<>(body, headers), Map.class);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String importId = (String) accepted.getBody().get("importId");
        assertThat(importId).isNotNull();

        // Poll the async job to completion.
        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(adminToken());
        Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<Map> status = rest.exchange(
                            "/api/v1/products/import/" + importId,
                            org.springframework.http.HttpMethod.GET, new HttpEntity<>(auth), Map.class);
                    assertThat(status.getBody().get("status")).isEqualTo("COMPLETED");
                });

        ResponseEntity<Map> result = rest.exchange("/api/v1/products/import/" + importId,
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(auth), Map.class);
        Map<String, Object> b = result.getBody();
        // GOOD-1 inserted then updated (same SKU) -> 1 imported + 1 updated;
        // GOOD-2 imported. free/negative/no-name -> 3 skipped. Empty trailing row ignored.
        assertThat(((Number) b.get("imported")).intValue()).isEqualTo(2);
        assertThat(((Number) b.get("updated")).intValue()).isEqualTo(1);
        assertThat(((Number) b.get("skipped")).intValue()).isEqualTo(3);
        assertThat((java.util.List<?>) b.get("errors")).hasSize(3);
    }
}
