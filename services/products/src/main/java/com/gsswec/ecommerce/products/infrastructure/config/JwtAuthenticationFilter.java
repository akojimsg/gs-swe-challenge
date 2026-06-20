package com.gsswec.ecommerce.products.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Verification-only JWT filter. Products does not issue tokens; it validates the
// access token minted by the Users service using the shared signing secret, so it
// can authorize its own [ADMIN] write endpoints (defence in depth per the auth spec).
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(JwtProperties properties) {
        this.signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                Claims claims = Jwts.parser().verifyWith(signingKey).build()
                        .parseSignedClaims(header.substring(BEARER_PREFIX.length()))
                        .getPayload();
                var authority = new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class));
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid token → leave the context unauthenticated; security rules
                // reject protected routes downstream.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
