package com.example.inventoryservice.repositories;

import com.example.inventoryservice.entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    ToolEntity findByid(long id);

    List<ToolEntity> findBycategory(String category);
    List<ToolEntity> findAllByinitialState(String category);
    List<ToolEntity> findAll();
    List<ToolEntity> findByname(String name);
    List<ToolEntity> findBytoolValue(double loans);

}