package com.example.loanservice.controllers;

import com.example.loanservice.entities.LoanEntity;
import com.example.loanservice.repositories.LoanRepository;
import com.example.loanservice.services.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loan")
@CrossOrigin("*")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanRepository loanRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createLoan(@RequestBody LoanEntity loan) {
        try {
            LoanEntity savedLoan = loanService.createLoan(
                    loan,
                    loan.getRutCustomer(),
                    loan.getToolNames(),
                    loan.getStartDate(),
                    loan.getDueDate()
            );
            return ResponseEntity.ok(savedLoan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/return/{idLoan}")
    public ResponseEntity<?> returnTools(
            @PathVariable long idLoan,
            @RequestParam(defaultValue = "0") double dailyLateFee, // ya no se usa en Service, pero se deja
            @RequestParam(defaultValue = "0") double repairCost,
            @RequestParam(value = "damaged", required = false) List<String> damagedTools,
            @RequestParam(value = "discarded", required = false) List<String> discardedTools
    ) {
        try {
            LoanEntity updatedLoan = loanService.returnTools(
                    idLoan,
                    dailyLateFee,
                    repairCost,
                    damagedTools != null ? damagedTools : new ArrayList<>(),
                    discardedTools != null ? discardedTools : new ArrayList<>()
            );
            return ResponseEntity.ok(updatedLoan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{idLoan}/pay")
    public ResponseEntity<?> payLoan(@PathVariable long idLoan) {
        try {
            LoanEntity updated = loanService.markLoanAsPaid(idLoan);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-loans")
    public ResponseEntity<?> getMyLoans(@RequestParam String rut) {
        try {
            List<LoanEntity> loans = loanService.findLoansByCustomerRut(rut);
            return ResponseEntity.ok(loans);
        } catch (Exception e) {

            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/all-loans")
    public ResponseEntity<?> getAllLoans() {
        try {
            return ResponseEntity.ok(loanRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{idLoan}")
    public ResponseEntity<?> getLoanById(@PathVariable long idLoan) {
        try {
            LoanEntity loan = loanRepository.findById(idLoan)
                    .orElseThrow(() -> new IllegalArgumentException("Préstamo no encontrado con ID: " + idLoan));
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //PARA EL REPORTING

    @GetMapping("/active/current")
    public ResponseEntity<?> currentActiveLoans() {
        try {
            return ResponseEntity.ok(
                    loanRepository.findByEndDateIsNullAndDueDateGreaterThanEqual(LocalDate.now())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF6.1: préstamos activos agrupados (vigentes/atrasos)
    // filtros opcionales: from/to (por startDate)
    @GetMapping("/active/grouped")
    public ResponseEntity<?> activeLoansGrouped(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        try {
            LocalDate f = (from != null && !from.isBlank()) ? LocalDate.parse(from) : null;
            LocalDate t = (to != null && !to.isBlank()) ? LocalDate.parse(to) : null;

            Map<String, List<LoanEntity>> result = loanService.listActiveLoansGrouped(f, t);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF6.2: loans activos atrasados (reporting obtiene ruts desde acá)
    @GetMapping("/active/overdue")
    public ResponseEntity<?> overdueActiveLoans(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        try {
            LocalDate f = (from != null && !from.isBlank()) ? LocalDate.parse(from) : null;
            LocalDate t = (to != null && !to.isBlank()) ? LocalDate.parse(to) : null;

            return ResponseEntity.ok(loanService.listOverdueActiveLoans(f, t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
