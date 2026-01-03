package com.example.kardexservice.services;

import com.example.kardexservice.entities.KardexEntity;
import com.example.kardexservice.repositories.KardexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class KardexService {

    @Autowired
    private KardexRepository kardexRepository;

    public KardexEntity saveMovement(KardexEntity movement) {
        if (movement == null) throw new IllegalArgumentException("Movimiento inválido");
        if (movement.getRutCustomer() == null || movement.getRutCustomer().isBlank())
            throw new IllegalArgumentException("rutCustomer es requerido");
        if (movement.getMovementType() == null || movement.getMovementType().isBlank())
            throw new IllegalArgumentException("movementType es requerido");
        if (movement.getToolName() == null || movement.getToolName().isBlank())
            throw new IllegalArgumentException("toolName es requerido");
        if (movement.getToolQuantity() <= 0)
            throw new IllegalArgumentException("toolQuantity debe ser > 0");

        if (movement.getMovementDate() == null) {
            movement.setMovementDate(LocalDate.now());
        }

        return kardexRepository.save(movement);
    }
}
