package com.example.pricingservice.repositories;

import com.example.pricingservice.entities.PricingConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfigEntity,Long> {
    
    public List<PricingConfigEntity> findAll();
}
