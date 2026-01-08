package com.example.customerservice.controllers;

import com.example.customerservice.entities.CustomerEntity;
import com.example.customerservice.services.CustomerService;
import com.example.customerservice.repositories.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer")
@CrossOrigin("*")
public class CustomerController {

    @Autowired
    CustomerService customerService;
    @Autowired
    CustomerRepository customerRepository;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // 1) Crear cliente
    // POST /api/v1/customer
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CustomerEntity customer) {
        try {
            CustomerEntity created = customerService.createCustomer(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // 2) Obtener por RUT
    // GET /api/v1/customer/{rut}
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/{rut}")
    public ResponseEntity<?> getByRut(@PathVariable String rut) {
        try {
            CustomerEntity customer = customerService.findByRutOrThrow(rut);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    // 3) Listar todos
    // GET /api/v1/customer
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<CustomerEntity>> getAll() {
        return ResponseEntity.ok(customerService.findAll());
    }

    // 4) Cambiar status
    // PUT /api/v1/customer/{rut}/status?status=Activo
    // PUT /api/v1/customer/{rut}/status?status=Restringido
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{rut}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String rut,
                                          @RequestParam String status) {
        // Validación mínima (opcional)
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body("Debes enviar el parámetro 'status'.");
        }
        if (!status.equalsIgnoreCase("Activo") && !status.equalsIgnoreCase("Restringido")) {
            return ResponseEntity.badRequest().body("Estado inválido. Usa: Activo o Restringido.");
        }

        try {
            String normalized = status.equalsIgnoreCase("Activo") ? "Activo" : "Restringido";
            CustomerEntity updated = customerService.setStatus(rut, normalized);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }


    // NO SE SI DEJAN ACÁ

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/findCustomer")

    public ResponseEntity<?> findByRut(@RequestParam String rut) {
        try {
            CustomerEntity customer = customerRepository.findByRut(rut);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // clients with overdue, NO LO ESTOY DEJANDO ACÁ

    @GetMapping("/allGreatherThan")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> customerGreatherThan(@RequestParam int quantity) {
        try {
            List<CustomerEntity> customers = customerRepository.findByquantityLoansGreaterThan(quantity);
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6) Actualizar cantidad de préstamos (delta)
// PUT /api/v1/customer/{rut}/loans?delta=1
// PUT /api/v1/customer/{rut}/loans?delta=-1
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{rut}/loans")
    public ResponseEntity<?> updateLoans(@PathVariable String rut, @RequestParam int delta) {
        try {
            CustomerEntity updated = customerService.updateQuantityLoans(rut, delta);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllCustomers() {
        try {
            List<CustomerEntity> customers = customerRepository.findAll();
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}
