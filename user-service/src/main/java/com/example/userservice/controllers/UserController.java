package com.example.userservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin("*")
public class UserController {

    @GetMapping("/public/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok("ok");
    }

    // El gateway inyecta estos headers desde el JWT
    @GetMapping("/api/v1/auth/me")
    public ResponseEntity<?> me(
            @RequestHeader(value = "X-User-Sub", required = false) String sub,
            @RequestHeader(value = "X-User-Username", required = false) String username,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Rut", required = false) String rut,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesCsv
    ) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sub", sub);
        out.put("username", username);
        out.put("email", email);
        out.put("rut", rut);

        List<String> roles = new ArrayList<>();
        if (rolesCsv != null && !rolesCsv.isBlank()) {
            roles.addAll(Arrays.asList(rolesCsv.split(",")));
        }
        out.put("roles", roles);

        return ResponseEntity.ok(out);
    }
}
