package com.gsswec.ecommerce.notifications.infrastructure.users;

import com.gsswec.ecommerce.notifications.application.port.out.UserDirectory;
import com.gsswec.ecommerce.notifications.infrastructure.config.NotificationsProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestUserDirectory implements UserDirectory {

    private static final Logger log = LoggerFactory.getLogger(RestUserDirectory.class);

    private final RestClient restClient;
    private final Duration cacheTtl;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    public RestUserDirectory(RestClient.Builder builder, NotificationsProperties properties) {
        this.restClient = builder.baseUrl(properties.usersBaseUrl()).build();
        this.cacheTtl = Duration.ofMinutes(properties.userCacheTtlMinutes());
    }

    @Override
    public Optional<String> emailFor(UUID userId) {
        Cached hit = cache.get(userId);
        if (hit != null && hit.expiresAt.isAfter(Instant.now())) {
            return Optional.ofNullable(hit.email);
        }
        Optional<String> resolved = fetch(userId);
        resolved.ifPresent(email -> cache.put(userId, new Cached(email, Instant.now().plus(cacheTtl))));
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private Optional<String> fetch(UUID userId) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/internal/users/{id}/email", userId)
                    .retrieve()
                    .body(Map.class);
            String email = body == null ? null : (String) body.get("email");
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(email);
        } catch (Exception e) {
            log.debug("Could not resolve email for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private record Cached(String email, Instant expiresAt) {
    }
}
