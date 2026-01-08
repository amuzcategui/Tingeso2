package com.example.userservice.controllers;

import com.example.userservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:5173", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserController {

    @Autowired
    private UserService userService;

    // POST /api/v1/users/check-and-create
    // Requiere Authorization: Bearer <token>
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/check-and-create")
    public ResponseEntity<?> checkAndCreate(@AuthenticationPrincipal Jwt jwt) {

        // Si no hay JWT, es 401 (no 500)
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No hay JWT en el contexto (Authorization Bearer token requerido).");
        }

        try {
            Map<String, Object> customer = userService.checkAndCreateCustomerFromJwt(jwt);
            return ResponseEntity.ok(customer);

        } catch (IllegalArgumentException e) {
            // Errores de validación / 4xx del customer-service que reenviamos como mensaje
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            // Para debug, muchas veces conviene devolver una respuesta genérica y loggear el detalle
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno en user-service: " + e.getMessage());
        }
    }
}
