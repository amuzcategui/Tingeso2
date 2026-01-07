package com.example.apigateway.filters;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class JwtToHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        Authentication auth = exchange.getPrincipal().cast(Authentication.class).blockOptional().orElse(null);

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            String sub = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");

            // Si en Keycloak creas un mapper para "rut"
            String rut = jwt.getClaimAsString("rut");

            String rolesCsv = jwtAuth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.joining(","));

            ServerHttpRequest req = exchange.getRequest().mutate()
                    .header("X-User-Sub", sub != null ? sub : "")
                    .header("X-User-Username", username != null ? username : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Rut", rut != null ? rut : "")
                    .header("X-User-Roles", rolesCsv)
                    .build();

            return chain.filter(exchange.mutate().request(req).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Antes de que se rutee
        return -1;
    }
}
