package com.example.pricingservice.services;

import com.example.pricingservice.entities.PricingConfigEntity;
import com.example.pricingservice.repositories.PricingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PricingConfigService {

    @Autowired
    private PricingConfigRepository pricingConfigRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Service name registered in Eureka
    private static final String INVENTORY_SERVICE_URL = "http://inventory-service/api/v1/tools";

    // ------------------ Helpers ------------------

    private PricingConfigEntity getOrCreateConfig() {
        List<PricingConfigEntity> all = pricingConfigRepository.findAll();
        if (all.isEmpty()) {
            PricingConfigEntity cfg = new PricingConfigEntity();
            cfg.setRentalFeeDaily(0);
            return pricingConfigRepository.save(cfg);
        }
        return all.get(0);
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    // ------------------ Config general ------------------

    @Transactional(readOnly = true)
    public PricingConfigEntity getConfig() {
        return getOrCreateConfig();
    }

    @Transactional(readOnly = true)
    public double getRentalFeeDailyValue() {
        return getOrCreateConfig().getRentalFeeDaily();
    }

    @Transactional
    public PricingConfigEntity updateRentalFeeDaily(double newValue) {
        if (newValue < 0) throw new IllegalArgumentException("rentalFeeDaily inválido");
        PricingConfigEntity cfg = getOrCreateConfig();
        cfg.setRentalFeeDaily(newValue);
        return pricingConfigRepository.save(cfg);
    }

    // ------------------ RF4.3 Valor reposición por herramienta (inventory-service) ------------------

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateToolValue(Long idTool, double newToolValue) {
        if (idTool == null) throw new IllegalArgumentException("idTool es requerido");
        if (newToolValue <= 0) throw new IllegalArgumentException("toolValue inválido");

        String url = INVENTORY_SERVICE_URL + "/" + idTool + "/pricing/value";

        Map<String, Object> body = new HashMap<>();
        body.put("toolValue", newToolValue);

        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.PUT, jsonEntity(body), Map.class);
        return (Map<String, Object>) res.getBody();
    }
}
