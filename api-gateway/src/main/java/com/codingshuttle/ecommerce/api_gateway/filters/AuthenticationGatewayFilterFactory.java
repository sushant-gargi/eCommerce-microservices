package com.codingshuttle.ecommerce.api_gateway.filters;

import com.codingshuttle.ecommerce.api_gateway.service.JwtService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtService jwtService;

    public AuthenticationGatewayFilterFactory(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            // 🔹 Skip if disabled
            if (!config.enabled) {
                return chain.filter(exchange);
            }

            // 🔹 Get Authorization header
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst("Authorization");

            // 🔹 Validate token presence
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            Long userId = jwtService.getUserIdFromToken(token);
            List<String> roles = jwtService.getUserRoleFromToken(token);

            if (config.getAllowedRoles() != null &&
                    roles.stream().noneMatch(config.getAllowedRoles()::contains)) {

                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId.toString())
                    .header("X-User-Roles", String.join(",", roles))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    @Data
    public static class Config {
        private boolean enabled;
        private List<String> allowedRoles;
    }
}
