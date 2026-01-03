package com.example.reportingservice.services;

import com.example.reportingservice.entities.ReportingEntity;
import com.example.reportingservice.repositories.ReportingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportingService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ReportingRepository reportCacheRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Eureka service names ---
    private static final String LOAN_SERVICE = "http://loan-service/api/v1/loan";
    private static final String CUSTOMER_SERVICE = "http://customer-service/api/v1/customer";
    private static final String KARDEX_SERVICE = "http://kardex-service/api/v1/kardex";

    // ---------------- RF6.1: préstamos activos agrupados ----------------
    // Llama a loan-service: GET /active/grouped?from=&to=
    public Object activeLoansGrouped(String from, String to) {

        String url = LOAN_SERVICE + "/active/grouped" + buildQuery(from, to);
        Object result = restTemplate.getForObject(url, Object.class);

        saveCache("ACTIVE_GROUPED", from, to, result);
        return result;
    }

    // ---------------- RF6.2: clientes con atrasos ----------------
    // 1) loan-service devuelve loans atrasados (lista)
    // 2) extraer ruts únicos
    // 3) customer-service GET /{rut} por cada uno
    public List<Object> overdueCustomers(String from, String to) {

        String url = LOAN_SERVICE + "/active/overdue" + buildQuery(from, to);

        Object[] overdueLoans = restTemplate.getForObject(url, Object[].class);
        if (overdueLoans == null) overdueLoans = new Object[0];

        // Extraer ruts únicos desde loans (sin DTO, entonces convertimos via Map)
        Set<String> ruts = new LinkedHashSet<>();

        for (Object o : overdueLoans) {
            if (o instanceof Map) {
                Object rut = ((Map<?, ?>) o).get("rutCustomer");
                if (rut != null) ruts.add(rut.toString());
            } else {
                // fallback: intentar serializar y leer
                Map<?, ?> m = objectMapper.convertValue(o, Map.class);
                Object rut = m.get("rutCustomer");
                if (rut != null) ruts.add(rut.toString());
            }
        }

        List<Object> customers = new ArrayList<>();
        for (String rut : ruts) {
            try {
                // Usa tu endpoint RESTFUL: GET /api/v1/customer/{rut}
                Object cust = restTemplate.getForObject(CUSTOMER_SERVICE + "/" + rut, Object.class);
                if (cust != null) customers.add(cust);
            } catch (Exception ignored) {
                // si algún rut no existe o falla customer-service, no rompemos el reporte
            }
        }

        saveCache("OVERDUE_CUSTOMERS", from, to, customers);
        return customers;
    }

    // ---------------- RF6.3: ranking herramientas más prestadas ----------------
    // Llama a kardex-service: GET /tools/top?from=&to=&limit=
    public Object topLoanedTools(String from, String to, Integer limit) {

        StringBuilder sb = new StringBuilder();
        sb.append(KARDEX_SERVICE).append("/tools/top?");

        boolean first = true;
        if (from != null && !from.isBlank()) {
            sb.append("from=").append(from);
            first = false;
        }
        if (to != null && !to.isBlank()) {
            if (!first) sb.append("&");
            sb.append("to=").append(to);
            first = false;
        }
        if (limit != null && limit > 0) {
            if (!first) sb.append("&");
            sb.append("limit=").append(limit);
        }

        Object result = restTemplate.getForObject(sb.toString(), Object.class);

        saveCache("TOP_TOOLS", from, to, result);
        return result;
    }

    // ---------------- Cache helpers ----------------

    private String buildQuery(String from, String to) {
        boolean hasFrom = from != null && !from.isBlank();
        boolean hasTo = to != null && !to.isBlank();

        if (!hasFrom && !hasTo) return "";

        StringBuilder sb = new StringBuilder("?");
        if (hasFrom) sb.append("from=").append(from);
        if (hasTo) {
            if (hasFrom) sb.append("&");
            sb.append("to=").append(to);
        }
        return sb.toString();
    }

    private void saveCache(String type, String from, String to, Object payloadObj) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObj);

            ReportingEntity cache = new ReportingEntity();
            cache.setReportType(type);
            cache.setFromDate(from);
            cache.setToDate(to);
            cache.setGeneratedAt(LocalDateTime.now());
            cache.setPayload(payload);

            reportCacheRepository.save(cache);
        } catch (Exception ignored) {
            // si falla la caché no debe romper el reporte
        }
    }

    public Object lastCached(String reportType) {
        return reportCacheRepository
                .findTopByReportTypeOrderByGeneratedAtDesc(reportType)
                .map(ReportingEntity::getPayload)
                .orElse(null);
    }
}
