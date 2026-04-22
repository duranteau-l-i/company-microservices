package com.company.apigateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator jwtTokenValidator;
    private final SecurityProperties securityProperties;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator, SecurityProperties securityProperties) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.securityProperties = securityProperties;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        Claims claims;
        try {
            claims = jwtTokenValidator.validateAndExtractClaims(token);
        } catch (RuntimeException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        }

        if (!jwtTokenValidator.isAccessToken(claims)) {
            log.debug("Token is not an access token");
            return unauthorized(exchange);
        }

        String role = claims.get("role", String.class);
        if (role == null) {
            log.debug("Token is missing role claim");
            return unauthorized(exchange);
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return securityProperties.getPublicPaths().stream()
                .anyMatch(p -> {
                    String base = p.endsWith("/**") ? p.substring(0, p.length() - 3) : p;
                    return path.equals(base) || path.startsWith(base + "/");
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
