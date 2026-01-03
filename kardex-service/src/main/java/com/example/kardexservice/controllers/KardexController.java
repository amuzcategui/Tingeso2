package com.example.kardexservice.controllers;

import com.example.kardexservice.entities.KardexEntity;
import com.example.kardexservice.repositories.KardexRepository;
import com.example.kardexservice.services.KardexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kardex")
@CrossOrigin("*")
public class KardexController {

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private KardexService kardexService;

    // ✅ ESTE endpoint lo usa inventory-service:
    // POST http://kardex-service/api/v1/kardex/movements
    @PostMapping("/movements")
    public ResponseEntity<?> createMovement(@RequestBody KardexEntity movement) {
        try {
            return ResponseEntity.ok(kardexService.saveMovement(movement));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF5.2: History by tool (consulta simple)
    @GetMapping("/tool-history")
    public ResponseEntity<?> toolHistory(@RequestParam String toolName) {
        try {
            if (toolName == null || toolName.isBlank()) {
                return ResponseEntity.badRequest().body("toolName es requerido");
            }
            return ResponseEntity.ok(kardexRepository.findBytoolName(toolName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF5.3: Movements by range (consulta simple)
    @GetMapping("/range")
    public ResponseEntity<?> movementsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam String movementType
    ) {
        try {
            if (to.isBefore(from)) {
                return ResponseEntity.badRequest().body("El rango de fechas es inválido (to < from)");
            }
            return ResponseEntity.ok(
                    kardexRepository.findByMovementTypeAndMovementDateBetween(movementType, from, to)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> allKardex() {
        try {
            return ResponseEntity.ok(kardexRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
