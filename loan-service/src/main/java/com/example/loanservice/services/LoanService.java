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
    private static final String PRICE_RENTAL_FEE_DAILY = PRICING_BASE + "/rental-fee-daily";

    // ---------- Customer endpoints ----------
    private static final String CUST_GET_BY_RUT   = CUSTOMER_BASE + "/{rut}";
    private static final String CUST_UPDATE_LOANS = CUSTOMER_BASE + "/{rut}/loans?delta={delta}";
    private static final String CUST_SET_STATUS   = CUSTOMER_BASE + "/{rut}/status?status={status}";

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

    private void customerSetStatus(String rut, String status) {
        try {
            restTemplate.exchange(CUST_SET_STATUS, HttpMethod.PUT, null, Object.class, rut, status);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo actualizar status en customer-service", e);
        }
    }

    private int getCustomerQuantityLoans(Map<String, Object> customerJson) {
        try {
            return Integer.parseInt(String.valueOf(customerJson.getOrDefault("quantityLoans", "0")));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Actualiza status en customer-service usando:
     * - overdue activo OR deuda impaga OR quantityLoans >= 5 => Restringido
     * - si no => Activo
     */
    private void syncCustomerStatus(String rut) {
        Map<String, Object> customer = customerGetOrThrow(rut);
        int qLoans = getCustomerQuantityLoans(customer);

        boolean hasOverdue = loanRepository
                .existsByRutCustomerAndEndDateIsNullAndDueDateBefore(rut, LocalDate.now());

        boolean hasUnpaid = loanRepository
                .existsByRutCustomerAndPaidIsFalseAndEndDateNotNull(rut);

        if (hasOverdue || hasUnpaid || qLoans >= 5) {
            customerSetStatus(rut, "Restringido");
        } else {
            customerSetStatus(rut, "Activo");
        }
    }

    /**
     * Bloquea creación de préstamo si:
     * - overdue activo OR deuda impaga OR quantityLoans >= 5
     * Además intenta dejar el status como Restringido para consistencia.
     */
    private void validateCustomerAllowedToLoan(String rut) {
        Map<String, Object> customer = customerGetOrThrow(rut);
        int qLoans = getCustomerQuantityLoans(customer);

        boolean hasOverdue = loanRepository
                .existsByRutCustomerAndEndDateIsNullAndDueDateBefore(rut, LocalDate.now());

        boolean hasUnpaid = loanRepository
                .existsByRutCustomerAndPaidIsFalseAndEndDateNotNull(rut);

        if (hasOverdue || hasUnpaid || qLoans >= 5) {
            try { customerSetStatus(rut, "Restringido"); } catch (Exception ignored) {}
            if (qLoans >= 5) {
                throw new IllegalArgumentException("El cliente no puede tener más de 5 préstamos activos");
            }
            throw new IllegalArgumentException("El cliente no está activo (tiene atrasos o deudas sin pagar)");
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
    //  PRICING helper (SOLO rentalFeeDaily)
    // =========================================================================

    private double pricingGetRentalFeeDaily() {
        try {
            ResponseEntity<Double> resp = restTemplate.getForEntity(PRICE_RENTAL_FEE_DAILY, Double.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) return resp.getBody();
            throw new RuntimeException("pricing-service no devolvió rentalFeeDaily");
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener rentalFeeDaily en pricing-service", e);
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
    //  Cálculos
    // =========================================================================

    private double calculateFee(LocalDate startDate, LocalDate dueDate, double rentalDailyFee) {
        double totalDays = ChronoUnit.DAYS.between(startDate, dueDate);
        return totalDays * rentalDailyFee;
    }

    private double calculateLateFee(LocalDate dueDate, LocalDate endDate, double dailyLateFee) {
        double latefee = 0;
        if (dueDate != null && endDate.isAfter(dueDate)) {
            double totalDaysLate = ChronoUnit.DAYS.between(dueDate, endDate);
            latefee = totalDaysLate * dailyLateFee;
        }
        return latefee;
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

        // ✅ validación completa (atrasos / deudas / límite 5)
        validateCustomerAllowedToLoan(rutCustomer);

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

            // fee global por préstamo
            double rentalFeeDaily = pricingGetRentalFeeDaily();
            double totalRentalFee = calculateFee(startDate, dueDate, rentalFeeDaily);

            // actualizar quantityLoans +1
            customerUpdateLoans(rutCustomer, +1);

            // ✅ sincronizar status (si llega a 5, queda Restringido; si no y sin deudas/atrasos, Activo)
            syncCustomerStatus(rutCustomer);

            loan.setRutCustomer(rutCustomer);
            loan.setToolNames(namesForLoan);
            loan.setStartDate(startDate);
            loan.setDueDate(dueDate);
            loan.setEndDate(null);
            loan.setFine(0.0);
            loan.setRentalFee(totalRentalFee);
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
            double dailyLateFee,
            double repairCost,
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

        // multa calculada con dailyLateFee recibido
        double lateFee = calculateLateFee(dueDate, endDate, dailyLateFee);
        loan.setFine(lateFee);

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

        // actualizar quantityLoans -1
        customerUpdateLoans(rutCustomer, -1);

        // ✅ sincronizar status (si bajó de 5 y no tiene deudas/atrasos, Activo)
        syncCustomerStatus(rutCustomer);

        return loanRepository.save(loan);
    }

    @Transactional
    public LoanEntity markLoanAsPaid(long idLoan) {
        LoanEntity loan = loanRepository.findByid(idLoan);
        if (loan == null) throw new IllegalArgumentException("Préstamo no encontrado");

        loan.setPaid(true);
        LoanEntity saved = loanRepository.save(loan);

        // ✅ al pagar puede quitarse la restricción por deuda
        syncCustomerStatus(saved.getRutCustomer());

        return saved;
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
