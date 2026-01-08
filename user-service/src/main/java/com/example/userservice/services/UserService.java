package com.example.userservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String CUSTOMER_BASE = "http://customer-service/api/v1/customer";
    private static final String CUST_GET_BY_RUT = CUSTOMER_BASE + "/{rut}";
    private static final String CUST_CREATE     = CUSTOMER_BASE; // POST /api/v1/customer

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
        Map<String, Object> existing = tryGetCustomer(rut);
        if (existing != null) return existing;

        // 2) si no existe, crearlo con Map (sin DTO)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rut", rut);
        body.put("email", jwt.getClaimAsString("email"));

        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        String fullName = joinName(given, family);

        body.put("name", fullName);

        // si no tienes phone en KC, puede venir null -> ajusta el customer-service para permitirlo
        body.put("phone", jwt.getClaimAsString("phone"));

        String birthdate = jwt.getClaimAsString("birthdate");
        if (birthdate != null && !birthdate.isBlank()) {
            // CustomerEntity suele ser LocalDate, Jackson acepta yyyy-MM-dd si el campo se llama birthDate
            body.put("birthDate", birthdate);
        }

        // admin desde roles
        body.put("admin", hasRole(jwt, "ADMIN"));

        // defaults del negocio
        body.put("status", "Activo");
        body.put("quantityLoans", 0);
        body.put("password", null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> created = restTemplate.exchange(CUST_CREATE, HttpMethod.POST, req, Map.class);
            if (created.getStatusCode().is2xxSuccessful() && created.getBody() != null) {
                return (Map<String, Object>) created.getBody();
            }
            throw new RuntimeException("customer-service no devolvió el cliente creado");
        } catch (HttpClientErrorException e) {
            // si el customer-service devuelve 400 con un mensaje, lo pasamos tal cual
            throw new IllegalArgumentException(e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear el cliente en customer-service", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tryGetCustomer(String rut) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(CUST_GET_BY_RUT, Map.class, rut);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return (Map<String, Object>) resp.getBody();
            }
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
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
