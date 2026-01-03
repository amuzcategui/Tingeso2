package com.example.kardexservice.repositories;

import com.example.kardexservice.entities.KardexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    KardexEntity findByid(long id);

    List<KardexEntity> findAll();

    List<KardexEntity> findBymovementType(String movement);

    List<KardexEntity> findByrutCustomer(String rutCustomer);

    List<KardexEntity> findBytoolName(String toolName);

    List<KardexEntity> findByMovementTypeAndMovementDateBetween(String movementType, LocalDate from, LocalDate to);
}
