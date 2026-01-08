package com.example.userservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String CUSTOMER_BASE    = "http://customer-service/api/v1/customer";
    private static final String CUST_GET_BY_RUT  = CUSTOMER_BASE + "/{rut}";
    private static final String CUST_CREATE      = CUSTOMER_BASE; // POST /api/v1/customer

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkAndCreateCustomerFromJwt(Jwt jwt) {
        if (jwt == null) throw new IllegalArgumentException("No hay JWT en el contexto.");

        String rut = firstNonBlank(
                jwt.getClaimAsString("rut"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getSubject()
        );

        if (rut == null || rut.isBlank()) {
            throw new IllegalArgumentException("No se pudo obtener rut desde el token.");
        }

        // 1) si existe, devolverlo
        Map<String, Object> existing = tryGetCustomer(rut, jwt);
        if (existing != null) return existing;

        // 2) si no existe, crearlo
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rut", rut);
        body.put("email", jwt.getClaimAsString("email"));

        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        body.put("name", joinName(given, family));

        body.put("phone", jwt.getClaimAsString("phone"));

        String birthdate = jwt.getClaimAsString("birthdate");
        if (birthdate != null && !birthdate.isBlank()) {
            body.put("birthDate", birthdate);
        }

        body.put("admin", hasRole(jwt, "ADMIN"));
        body.put("status", "Activo");
        body.put("quantityLoans", 0);
        body.put("password", null);

        // ✅ IMPORTANTE: aquí sí mandamos Authorization Bearer
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, authHeaders(jwt));

        try {
            ResponseEntity<Map> created = restTemplate.exchange(CUST_CREATE, HttpMethod.POST, req, Map.class);
            if (created.getStatusCode().is2xxSuccessful() && created.getBody() != null) {
                return (Map<String, Object>) created.getBody();
            }
            throw new RuntimeException("customer-service no devolvió el cliente creado");
        } catch (HttpClientErrorException e) {
            // Si customer-service devuelve 4xx, devolvemos el body para debug
            throw new IllegalArgumentException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear el cliente en customer-service", e);
        }
    }

    private HttpHeaders authHeaders(Jwt jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt.getTokenValue());
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryGetCustomer(String rut, Jwt jwt) {
        try {
            HttpEntity<Void> req = new HttpEntity<>(authHeaders(jwt));
            ResponseEntity<Map> resp = restTemplate.exchange(
                    CUST_GET_BY_RUT,
                    HttpMethod.GET,
                    req,
                    Map.class,
                    rut
            );

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return (Map<String, Object>) resp.getBody();
            }
            return null;

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new RuntimeException("customer-service rechazó el token (401/403). Revisa roles/audience/issuer.", e);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo consultar customer-service (GET /customer/{rut})", e);
        }
    }

    private boolean hasRole(Jwt jwt, String role) {
        Object realmAccessObj = jwt.getClaims().get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> roles) {
                for (Object r : roles) {
                    if (role.equalsIgnoreCase(String.valueOf(r))) return true;
                }
            }
        }
        return false;
    }

    private String joinName(String given, String family) {
        String g = (given == null) ? "" : given.trim();
        String f = (family == null) ? "" : family.trim();
        String out = (g + " " + f).trim();
        return out.isBlank() ? "Sin nombre" : out;
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
