package com.example.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        http
                // Para APIs con JWT normalmente desactivas CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Autoriza preflight y actuator sin token
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        // (Opcional) deja públicos algunos endpoints si los necesitas:
                        .pathMatchers("/users/check-and-create").permitAll()

                        // Todo lo demás requiere JWT
                        .anyExchange().authenticated()
                )

                // Resource Server JWT (Keycloak)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }
}
