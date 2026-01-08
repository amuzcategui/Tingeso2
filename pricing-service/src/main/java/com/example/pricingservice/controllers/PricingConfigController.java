package com.example.pricingservice.controllers;

import com.example.pricingservice.entities.PricingConfigEntity;
import com.example.pricingservice.services.PricingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pricing")
@CrossOrigin("*")
public class PricingConfigController {

    @Autowired
    private PricingConfigService pricingService;

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        try {
            return ResponseEntity.ok(pricingService.getConfig());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Devuelve SOLO el número (double)
    @GetMapping("/rental-fee-daily")
    public ResponseEntity<?> getRentalFeeDaily() {
        try {
            return ResponseEntity.ok(pricingService.getRentalFeeDailyValue());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // body: { "rentalFeeDaily": 5000 }
    @PutMapping("/config/rental-fee-daily")
    public ResponseEntity<?> updateRentalFeeDaily(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("rentalFeeDaily")) {
                return ResponseEntity.badRequest().body("rentalFeeDaily es requerido");
            }
            double v = Double.parseDouble(body.get("rentalFeeDaily").toString());
            PricingConfigEntity cfg = pricingService.updateRentalFeeDaily(v);
            return ResponseEntity.ok(cfg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF4.3: actualizar valor reposición por tool (inventory-service)
    // body: { "toolValue": 15000 }
    @PutMapping("/tools/{idTool}/value")
    public ResponseEntity<?> updateToolValue(@PathVariable Long idTool, @RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("toolValue")) {
                return ResponseEntity.badRequest().body("toolValue es requerido");
            }
            double v = Double.parseDouble(body.get("toolValue").toString());
            return ResponseEntity.ok(pricingService.updateToolValue(idTool, v));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
