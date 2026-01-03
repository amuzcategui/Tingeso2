package com.example.loanservice.repositories;

import com.example.loanservice.entities.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    LoanEntity findByid(long id);

    List<LoanEntity> findByrutCustomer(String rutCustomer);

    // Activos (endDate null)
    List<LoanEntity> findByEndDateIsNull();

    // Activos atrasados (endDate null y dueDate < today)
    List<LoanEntity> findByEndDateIsNullAndDueDateBefore(LocalDate today);

    // Activos vigentes (endDate null y dueDate >= today)
    List<LoanEntity> findByEndDateIsNullAndDueDateGreaterThanEqual(LocalDate today);

    // Filtros por rango (startDate)
    List<LoanEntity> findByEndDateIsNullAndStartDateBetween(LocalDate from, LocalDate to);

    List<LoanEntity> findByEndDateIsNullAndDueDateBetween(LocalDate from, LocalDate to);

    // Restricciones de negocio usadas en createLoan
    boolean existsByRutCustomerAndEndDateIsNullAndDueDateBefore(String rutCustomer, LocalDate date);
    boolean existsByRutCustomerAndPaidIsFalseAndEndDateNotNull(String rutCustomer);
    boolean existsByRutCustomerAndPaidIsFalse(String rutCustomer);
    List<LoanEntity> findByRutCustomerAndEndDateIsNull(String rutCustomer);
}
