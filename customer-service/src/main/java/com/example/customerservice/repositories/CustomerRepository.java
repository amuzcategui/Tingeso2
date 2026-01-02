package com.example.customerservice.repositories;

import com.example.customerservice.entities.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByrut(String rut);
    public CustomerEntity findByRutI(String rut);
    List<CustomerEntity> findAll();
    List<CustomerEntity> findBystatus(String status);
    List<CustomerEntity> findByname(String name);
    List<CustomerEntity> findByquantityLoansGreaterThan(int loans);
}