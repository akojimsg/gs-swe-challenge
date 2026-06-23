package com.gsswec.ecommerce.gateway.infrastructure.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_HEADER = "X-Request-Id";
    public static final String TRACE_CONTEXT_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String existing = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        String traceId = (existing != null && !existing.isBlank())
                ? existing
                : UUID.randomUUID().toString();

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> h.set(TRACE_HEADER, traceId))
                .build();
        exchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);

        return chain.filter(exchange.mutate().request(mutated).build())
                .contextWrite(ctx -> ctx.put(TRACE_CONTEXT_KEY, traceId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
