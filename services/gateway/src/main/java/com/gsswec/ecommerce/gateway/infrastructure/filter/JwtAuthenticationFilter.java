package com.gsswec.ecommerce.gateway.infrastructure.filter;

import com.gsswec.ecommerce.gateway.infrastructure.config.GatewayPolicyProperties;
import com.gsswec.ecommerce.gateway.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

// Edge JWT validation (reactive WebFilter). Mirrors the services' verification
// logic (HS256 with the shared secret, claims sub/role) but in WebFlux form.
//
// - Requests matching the frozen public-routes allow-list bypass auth entirely.
// - Otherwise a valid Bearer token is required; verified claims are forwarded
//   downstream as X-User-Id / X-User-Role so services authorize without re-parsing.
// - Missing/invalid token on a protected route → 401 at the edge (request never
//   reaches the upstream).
@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    private final SecretKey signingKey;
    private final List<CompiledRoute> publicRoutes;

    public JwtAuthenticationFilter(JwtProperties jwtProperties, GatewayPolicyProperties policy) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.publicRoutes = policy.publicRoutes().stream()
                .map(r -> new CompiledRoute(
                        r.method() == null ? null : HttpMethod.valueOf(r.method().toUpperCase()),
                        PARSER.parse(r.path())))
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // The edge only authenticates the API surface. Non-/api paths (actuator
        // health/metrics, etc.) are infra, not routed business endpoints — let them
        // through untouched.
        String path = request.getPath().pathWithinApplication().value();
        if (!path.startsWith("/api/") || isPublic(request)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(header.substring(BEARER_PREFIX.length()))
                    .getPayload();

            // Forward identity downstream; strip any client-supplied spoof first.
            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> {
                        h.remove("X-User-Id");
                        h.remove("X-User-Role");
                        h.set("X-User-Id", claims.getSubject());
                        String role = claims.get("role", String.class);
                        if (role != null) {
                            h.set("X-User-Role", role);
                        }
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }
    }

    private boolean isPublic(ServerHttpRequest request) {
        var path = request.getPath().pathWithinApplication();
        HttpMethod method = request.getMethod();
        return publicRoutes.stream().anyMatch(r ->
                (r.method() == null || r.method().equals(method)) && r.pattern().matches(path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // Run before routing so unauthenticated protected requests never hit an upstream.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private record CompiledRoute(HttpMethod method, PathPattern pattern) {
    }
}
