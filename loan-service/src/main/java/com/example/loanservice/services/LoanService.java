package com.example.loanservice.services;

import com.example.loanservice.entities.LoanEntity;
import com.example.loanservice.repositories.LoanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private RestTemplate restTemplate;

    // ---------- Eureka service names ----------
    private static final String INVENTORY_BASE = "http://inventory-service/api/v1/tools";
    private static final String PRICING_BASE   = "http://pricing-service/api/v1/pricing";
    private static final String CUSTOMER_BASE  = "http://customer-service/api/v1/customer";

    // ---------- Inventory endpoints ----------
    private static final String INV_SEARCH_BY_NAME = INVENTORY_BASE + "/search?name={name}";
    private static final String INV_LOAN           = INVENTORY_BASE + "/{idTool}/loan?rutPerson={rut}&quantity={quantity}";
    private static final String INV_AVAILABLE      = INVENTORY_BASE + "/{idTool}/available?rutPerson={rut}&quantity={quantity}";
    private static final String INV_REPAIR         = INVENTORY_BASE + "/{idTool}/repair?rutPerson={rut}&quantity={quantity}";
    private static final String INV_DEACTIVATE     = INVENTORY_BASE + "/{idTool}/deactivate?rutPerson={rut}&quantity={quantity}";

    // ---------- Pricing endpoints ----------
    private static final String PRICE_CALC_LOAN    = PRICING_BASE + "/calculate/loan";
    private static final String PRICE_CALC_LATEFEE = PRICING_BASE + "/calculate/late-fee";

    // ---------- Customer endpoints ----------
    // En tu CustomerController YA tienes:
    // GET /api/v1/customer/{rut}
    // PUT /api/v1/customer/{rut}/loans?delta=1
    private static final String CUST_GET_BY_RUT     = CUSTOMER_BASE + "/{rut}";
    private static final String CUST_UPDATE_LOANS   = CUSTOMER_BASE + "/{rut}/loans?delta={delta}";

    // =========================================================================
    //  CUSTOMER helpers (JSON Map)
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> customerGetOrThrow(String rut) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(CUST_GET_BY_RUT, Map.class, rut);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalArgumentException("Cliente no encontrado");
            }
            return (Map<String, Object>) resp.getBody();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo conectar a customer-service (GET /customer/{rut})", e);
        }
    }

    private void customerUpdateLoans(String rut, int delta) {
        try {
            restTemplate.exchange(CUST_UPDATE_LOANS, HttpMethod.PUT, null, Object.class, rut, delta);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo actualizar quantityLoans en customer-service", e);
        }
    }

    private void validateCustomerActive(Map<String, Object> customerJson) {
        String status = String.valueOf(customerJson.getOrDefault("status", ""));
        if (!"Activo".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("El cliente no está activo");
        }
    }

    // =========================================================================
    //  INVENTORY helpers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> inventorySearchByName(String name) {
        try {
            ResponseEntity<List> resp = restTemplate.getForEntity(INV_SEARCH_BY_NAME, List.class, name);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return (List<Map<String, Object>>) resp.getBody();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo consultar inventory-service: /api/v1/tools/search", e);
        }
    }

    private void inventoryLoan(Long idTool, String rutPerson, int quantity) {
        try {
            restTemplate.exchange(INV_LOAN, HttpMethod.PUT, null, Object.class, idTool, rutPerson, quantity);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo prestar herramienta en inventory-service", e);
        }
    }

    private void inventoryAvailable(Long idTool, String rutPerson, int quantity) {
        try {
            restTemplate.exchange(INV_AVAILABLE, HttpMethod.PUT, null, Object.class, idTool, rutPerson, quantity);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo devolver herramienta a Disponible en inventory-service", e);
        }
    }

    private void inventoryRepair(Long idTool, String rutPerson, int quantity) {
        try {
            restTemplate.exchange(INV_REPAIR, HttpMethod.PUT, null, Object.class, idTool, rutPerson, quantity);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar a reparación en inventory-service", e);
        }
    }

    private void inventoryDeactivate(Long idTool, String rutPerson, int quantity) {
        try {
            restTemplate.exchange(INV_DEACTIVATE, HttpMethod.PUT, null, Object.class, idTool, rutPerson, quantity);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo dar de baja en inventory-service", e);
        }
    }

    // =========================================================================
    //  PRICING helpers
    // =========================================================================

    private double pricingCalculateLoan(int days) {
        Map<String, Object> body = new HashMap<>();
        body.put("days", days);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Double> resp = restTemplate.exchange(PRICE_CALC_LOAN, HttpMethod.POST, req, Double.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) return resp.getBody();
            throw new RuntimeException("pricing-service no devolvió rentalFee");
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular rentalFee en pricing-service", e);
        }
    }

    private double pricingCalculateLateFee(int lateDays) {
        Map<String, Object> body = new HashMap<>();
        body.put("lateDays", lateDays);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Double> resp = restTemplate.exchange(PRICE_CALC_LATEFEE, HttpMethod.POST, req, Double.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) return resp.getBody();
            return 0.0;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular lateFine en pricing-service", e);
        }
    }

    // =========================================================================
    //  JSON helpers
    // =========================================================================

    private boolean isLoanable(Map<String, Object> toolJson) {
        if (toolJson == null) return false;
        String state = String.valueOf(toolJson.getOrDefault("initialState", ""));
        int stock = Integer.parseInt(String.valueOf(toolJson.getOrDefault("stock", "0")));
        return "Disponible".equalsIgnoreCase(state) && stock >= 1;
    }

    private boolean isPrestada(Map<String, Object> toolJson) {
        if (toolJson == null) return false;
        String state = String.valueOf(toolJson.getOrDefault("initialState", ""));
        int stock = Integer.parseInt(String.valueOf(toolJson.getOrDefault("stock", "0")));
        return "Prestada".equalsIgnoreCase(state) && stock >= 1;
    }

    private Long getId(Map<String, Object> toolJson) {
        return Long.parseLong(String.valueOf(toolJson.get("id")));
    }

    private double getToolValue(Map<String, Object> toolJson) {
        return Double.parseDouble(String.valueOf(toolJson.getOrDefault("toolValue", "0")));
    }

    // =========================================================================
    //  FIRMAS EXACTAS
    // =========================================================================

    @Transactional
    public LoanEntity createLoan(
            LoanEntity loan,
            String rutCustomer,
            List<String> toolNames,
            LocalDate startDate,
            LocalDate dueDate
    ) {
        if (rutCustomer == null || rutCustomer.isBlank()) throw new IllegalArgumentException("Cliente no encontrado");
        if (toolNames == null || toolNames.isEmpty()) throw new IllegalArgumentException("Debe indicar herramientas");
        if (startDate == null || dueDate == null) throw new IllegalArgumentException("Fechas requeridas");
        if (dueDate.isBefore(startDate)) throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la de inicio");

        int days = (int) ChronoUnit.DAYS.between(startDate, dueDate);
        if (days < 1) throw new IllegalArgumentException("El arriendo debe ser mayor a un día");

        // ✅ 1) validar cliente existe + activo (equivalente a tu monolito)
        Map<String, Object> customer = customerGetOrThrow(rutCustomer);
        validateCustomerActive(customer);

        boolean hasOverdue = loanRepository.existsByRutCustomerAndEndDateIsNullAndDueDateBefore(rutCustomer, LocalDate.now());
        boolean hasUnpaid  = loanRepository.existsByRutCustomerAndPaidIsFalseAndEndDateNotNull(rutCustomer);
        if (hasOverdue || hasUnpaid) {
            throw new IllegalArgumentException("El cliente no está activo (tiene atrasos o deudas sin pagar)");
        }

        List<Long> loanedToolIds = new ArrayList<>();
        List<String> namesForLoan = new ArrayList<>();

        try {
            for (String toolName : toolNames) {
                if (toolName == null || toolName.isBlank()) continue;

                List<Map<String, Object>> tools = inventorySearchByName(toolName);

                Map<String, Object> chosen = null;
                for (Map<String, Object> t : tools) {
                    if (isLoanable(t)) { chosen = t; break; }
                }

                if (chosen == null) {
                    throw new IllegalArgumentException("No hay stock disponible para la herramienta: " + toolName);
                }

                Long idTool = getId(chosen);

                // prestar 1 unidad
                inventoryLoan(idTool, rutCustomer, 1);

                loanedToolIds.add(idTool);
                namesForLoan.add(toolName);
            }

            // rentalFee desde pricing-service
            double rentalFee = pricingCalculateLoan(days);

            // ✅ 2) actualizar quantityLoans +1 (equivalente a customer.setQuantityLoans+save)
            customerUpdateLoans(rutCustomer, +1);

            loan.setRutCustomer(rutCustomer);
            loan.setToolNames(namesForLoan);
            loan.setStartDate(startDate);
            loan.setDueDate(dueDate);
            loan.setEndDate(null);
            loan.setFine(0.0);
            loan.setRentalFee(rentalFee);
            loan.setPaid(false);

            return loanRepository.save(loan);

        } catch (Exception e) {
            // compensación: devolver lo prestado si falló a mitad
            for (Long idTool : loanedToolIds) {
                try { inventoryAvailable(idTool, rutCustomer, 1); } catch (Exception ignored) {}
            }
            throw e;
        }
    }

    @Transactional
    public LoanEntity returnTools(
            long idLoan,
            double dailyLateFee,   // firma exacta, ya NO se usa (lateFine viene de pricing-service)
            double repairCost,     // firma exacta
            List<String> damaged,
            List<String> discarded
    ) {
        LoanEntity loan = loanRepository.findByid(idLoan);
        if (loan == null) throw new IllegalArgumentException("Préstamo no encontrado");
        if (loan.isPaid()) throw new IllegalArgumentException("Ya el préstamo fue devuelto");

        if (damaged == null) damaged = new ArrayList<>();
        if (discarded == null) discarded = new ArrayList<>();

        loan.setDamagedTools(damaged);
        loan.setDiscardedTools(discarded);

        String rutCustomer = loan.getRutCustomer();
        LocalDate dueDate  = loan.getDueDate();
        LocalDate endDate  = LocalDate.now();

        loan.setEndDate(endDate);

        int lateDays = 0;
        if (dueDate != null && endDate.isAfter(dueDate)) {
            lateDays = (int) ChronoUnit.DAYS.between(dueDate, endDate);
        }

        // lateFine desde pricing-service
        double lateFine = pricingCalculateLateFee(lateDays);
        loan.setFine(lateFine);

        for (String toolName : loan.getToolNames()) {

            List<Map<String, Object>> tools = inventorySearchByName(toolName);

            Map<String, Object> prestada = null;
            for (Map<String, Object> t : tools) {
                if (isPrestada(t)) { prestada = t; break; }
            }

            if (prestada == null) {
                throw new IllegalArgumentException("No se encontró herramienta en estado 'Prestada': " + toolName);
            }

            Long idTool = getId(prestada);

            if (discarded.contains(toolName)) {
                inventoryDeactivate(idTool, rutCustomer, 1);
                loan.setFine(loan.getFine() + getToolValue(prestada)); // reposición

            } else if (damaged.contains(toolName)) {
                inventoryRepair(idTool, rutCustomer, 1);
                if (repairCost > 0) loan.setFine(loan.getFine() + repairCost);

            } else {
                inventoryAvailable(idTool, rutCustomer, 1);
            }
        }

        // ✅ 3) actualizar quantityLoans -1
        customerUpdateLoans(rutCustomer, -1);

        return loanRepository.save(loan);
    }

    @Transactional
    public LoanEntity markLoanAsPaid(long idLoan) {
        LoanEntity loan = loanRepository.findByid(idLoan);
        if (loan == null) throw new IllegalArgumentException("Préstamo no encontrado");
        loan.setPaid(true);
        return loanRepository.save(loan);
    }

    @Transactional
    public List<LoanEntity> listActiveLoans(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (to.isBefore(from)) throw new IllegalArgumentException("Rango inválido (to < from)");
            return loanRepository.findByEndDateIsNullAndStartDateBetween(from, to);
        }
        return loanRepository.findByEndDateIsNull();
    }

    @Transactional
    public List<LoanEntity> listOverdueActiveLoans(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();

        List<LoanEntity> base;
        if (from != null && to != null) {
            if (to.isBefore(from)) throw new IllegalArgumentException("Rango inválido (to < from)");
            base = loanRepository.findByEndDateIsNullAndStartDateBetween(from, to);
        } else {
            base = loanRepository.findByEndDateIsNull();
        }

        List<LoanEntity> out = new ArrayList<>();
        for (LoanEntity l : base) {
            if (l.getDueDate() != null && l.getDueDate().isBefore(today)) out.add(l);
        }
        return out;
    }

    @Transactional
    public Map<String, List<LoanEntity>> listActiveLoansGrouped(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();

        List<LoanEntity> active = listActiveLoans(from, to);

        List<LoanEntity> overdue = new ArrayList<>();
        List<LoanEntity> current = new ArrayList<>();

        for (LoanEntity l : active) {
            if (l.getDueDate() != null && l.getDueDate().isBefore(today)) overdue.add(l);
            else current.add(l);
        }

        Map<String, List<LoanEntity>> resp = new LinkedHashMap<>();
        resp.put("Atrasos", overdue);
        resp.put("Vigentes", current);
        return resp;
    }

    @Transactional
    public LoanEntity getLoanById(long idLoan) {
        LoanEntity loan = loanRepository.findByid(idLoan);
        if (loan == null) throw new IllegalArgumentException("Préstamo no encontrado");
        return loan;
    }

    @Transactional
    public List<LoanEntity> findLoansByCustomerRut(String rutCustomer) {
        return loanRepository.findByrutCustomer(rutCustomer);
    }
}
