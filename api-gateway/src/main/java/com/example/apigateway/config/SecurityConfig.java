package com.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Flux;

import java.util.*;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        // si tienes endpoints públicos (health, etc.)
                        .pathMatchers("/actuator/**").permitAll()

                        // ----- ADMIN -----
                        // pricing: configurar precios
                        .pathMatchers("/api/v1/pricing/**").hasRole("ADMIN")

                        // kardex completo (movimientos + consultas)
                        .pathMatchers("/api/v1/kardex/**").hasRole("ADMIN")

                        // reporting completo
                        .pathMatchers("/api/v1/reporting/**").hasRole("ADMIN")

                        // devolver préstamos (solo admin según lo que dijiste)
                        .pathMatchers("/api/v1/loan/return/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/loan/*/pay").hasRole("ADMIN") // pagar préstamo (si lo usas)

                        // opcional: endpoints “admin” de loan
                        .pathMatchers("/api/v1/loan/all-loans").hasRole("ADMIN")
                        .pathMatchers("/api/v1/loan/active/**").hasRole("ADMIN")

                        // ----- USER or ADMIN -----
                        .pathMatchers("/api/v1/loan/create").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1/loan/my-loans").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1/loan/{idLoan}").hasAnyRole("USER", "ADMIN")

                        // inventory / customer:
                        // ajusta según tu criterio (puedes dejarlos abiertos a USER+ADMIN)
                        .pathMatchers("/api/v1/tools/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1/tools/inventory/all").hasAnyRole("ADMIN")
                        .pathMatchers("/api/v1/customer/**").hasAnyRole("USER", "ADMIN")

                        // todo el resto bajo api
                        .pathMatchers("/api/v1/**").authenticated()

                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                )
                .build();
    }

    /**
     * Convierte realm_access.roles -> ROLE_X para que funcionen hasRole/hasAnyRole
     */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Object realmAccessObj = jwt.getClaims().get("realm_access");
            if (realmAccessObj instanceof Map<?, ?> realmAccess) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List<?> roles) {
                    for (Object r : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString()));
                    }
                }
            }

            return Flux.fromIterable(authorities);
        });
        return converter;
    }
}
