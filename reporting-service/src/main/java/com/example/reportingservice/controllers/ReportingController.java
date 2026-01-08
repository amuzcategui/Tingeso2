package com.example.reportingservice.controllers;

import com.example.reportingservice.services.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reporting")
@CrossOrigin("*")
public class ReportingController {

    @Autowired
    private ReportingService reportingService;

    // RF6.1: Préstamos activos agrupados (vigentes/atrasos)
    // Opcional: from/to (ISO yyyy-MM-dd) se pasan hacia loan-service
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/loans/active/grouped")
    public ResponseEntity<?> activeLoansGrouped(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        try {
            return ResponseEntity.ok(reportingService.activeLoansGrouped(from, to));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF6.2: Clientes con atrasos
    // reporting llama loan-service -> obtiene loans atrasados -> saca ruts -> llama customer-service
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/customers/overdue")
    public ResponseEntity<?> overdueCustomers(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        try {
            return ResponseEntity.ok(reportingService.overdueCustomers(from, to));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF6.3: Ranking herramientas más prestadas (usa kardex-service)
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/tools/top")
    public ResponseEntity<?> topTools(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            return ResponseEntity.ok(reportingService.topLoanedTools(from, to, limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // (Opcional) ver última caché
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/cache/last")
    public ResponseEntity<?> lastCache(@RequestParam String type) {
        try {
            return ResponseEntity.ok(reportingService.lastCached(type));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
