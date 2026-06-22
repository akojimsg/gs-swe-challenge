package com.gsswec.ecommerce.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

// Reactive (WebFlux) security — Spring Cloud Gateway is NOT servlet-based, so this
// uses ServerHttpSecurity / SecurityWebFilterChain, not HttpSecurity.
//
// Edge auth is enforced by JwtAuthenticationFilter (a reactive WebFilter that knows
// the frozen public-routes allow-list and returns 401 for protected routes without a
// valid token). This chain just disables the form/basic/csrf machinery and lets that
// filter run; actuator stays open for health/metrics.
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Authorization happens in JwtAuthenticationFilter against the frozen
                // public-routes list; permit all here so that filter is the single
                // source of truth (avoids double-maintaining the allow-list).
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }
}
