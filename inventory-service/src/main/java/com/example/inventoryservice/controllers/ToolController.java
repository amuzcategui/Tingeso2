package com.example.inventoryservice.controllers;

import com.example.inventoryservice.entities.ToolEntity;
import com.example.inventoryservice.services.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tools")
@CrossOrigin("*")
public class ToolController {

    @Autowired
    private ToolService toolService;

    // ------------------ RF1.1 Registrar nuevas herramientas ------------------
    // rutPerson: rut de quien realiza la operación (lo validas en el front)
    @PostMapping
    public ResponseEntity<?> createTool(
            @RequestBody ToolEntity tool,
            @RequestParam String rutPerson
    ) {
        try {
            return ResponseEntity.ok(toolService.saveTool(tool, rutPerson));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------ RF1.2 Dar de baja herramientas ------------------
    @PutMapping("/{idTool}/deactivate")
    public ResponseEntity<?> deactivateTool(
            @PathVariable Long idTool,
            @RequestParam String rutPerson,
            @RequestParam int quantity
    ) {
        try {
            return ResponseEntity.ok(toolService.deactivateTool(idTool, rutPerson, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------ Préstamo ------------------
    @PutMapping("/{idTool}/loan")
    public ResponseEntity<?> loanTool(
            @PathVariable Long idTool,
            @RequestParam String rutPerson,
            @RequestParam int quantity
    ) {
        try {
            return ResponseEntity.ok(toolService.loanTool(idTool, rutPerson, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------ Reparación ------------------
    @PutMapping("/{idTool}/repair")
    public ResponseEntity<?> repairTool(
            @PathVariable Long idTool,
            @RequestParam String rutPerson,
            @RequestParam int quantity
    ) {
        try {
            return ResponseEntity.ok(toolService.repairTool(idTool, rutPerson, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------ Volver a Disponible (devolución / reingreso) ------------------
    @PutMapping("/{idTool}/available")
    public ResponseEntity<?> availableTool(
            @PathVariable Long idTool,
            @RequestParam String rutPerson,
            @RequestParam int quantity
    ) {
        try {
            return ResponseEntity.ok(toolService.availableTool(idTool, rutPerson, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------ GET por ID (lo usa pricing-service para leer toolValue/rentalFee) ------------------
    @GetMapping("/{idTool}")
    public ResponseEntity<?> getToolById(@PathVariable Long idTool) {
        try {
            return ResponseEntity.ok(toolService.getToolById(idTool));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------ ENDPOINTS para pricing-service ------------------
    // Sin DTO: recibimos JSON simple (Map)

    // Body esperado: { "toolValue": 12345 }
    @PutMapping("/{idTool}/pricing/value")
    public ResponseEntity<?> updateToolValue(
            @PathVariable Long idTool,
            @RequestBody Map<String, Object> body
    ) {
        try {
            if (body == null || !body.containsKey("toolValue")) {
                return ResponseEntity.badRequest().body("toolValue es requerido");
            }
            double newValue = Double.parseDouble(body.get("toolValue").toString());
            ToolEntity updated = toolService.updateToolValue(idTool, newValue);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchByName(@RequestParam String name) {
        try {
            return ResponseEntity.ok(toolService.searchByName(name));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



}
