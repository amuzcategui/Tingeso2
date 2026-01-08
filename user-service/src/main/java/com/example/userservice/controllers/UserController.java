package com.example.userservice.controllers;

import com.example.userservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;

    // POST /api/v1/users/check-and-create
    // Requiere Authorization: Bearer <token>
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/check-and-create")
    public ResponseEntity<?> checkAndCreate(@AuthenticationPrincipal Jwt jwt) {
        try {
            Map<String, Object> customer = userService.checkAndCreateCustomerFromJwt(jwt);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
