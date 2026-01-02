package com.example.pricingservice.controllers;

import com.example.pricingservice.entities.PricingConfigEntity;
import com.example.pricingservice.services.PricingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/pricing")
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

    // body: { "lateFeeDaily": 1000 }
    @PutMapping("/config/late-fee-daily")
    public ResponseEntity<?> updateLateFeeDaily(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("lateFeeDaily")) {
                return ResponseEntity.badRequest().body("lateFeeDaily es requerido");
            }
            double v = Double.parseDouble(body.get("lateFeeDaily").toString());
            PricingConfigEntity cfg = pricingService.updateLateFeeDaily(v);
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

    // body: { "days": 3 }
    @PostMapping("/calculate/loan")
    public ResponseEntity<?> calculateLoan(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("days")) {
                return ResponseEntity.badRequest().body("days es requerido");
            }
            int days = Integer.parseInt(body.get("days").toString());
            return ResponseEntity.ok(pricingService.calculateLoanPrice(days));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // body: { "lateDays": 2 }
    @PostMapping("/calculate/late-fee")
    public ResponseEntity<?> calculateLateFee(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("lateDays")) {
                return ResponseEntity.badRequest().body("lateDays es requerido");
            }
            int lateDays = Integer.parseInt(body.get("lateDays").toString());
            return ResponseEntity.ok(pricingService.calculateLateFee(lateDays));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
