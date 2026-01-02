package com.example.inventoryservice.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;


@Entity
@Table(name = "tools")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    private String name;

    private double toolValue;
    // voy a hacer el rental fee general, no por herramienta.
    // El precio del loan va a ser rentalFee*días y va a estar en pricing-service
    //private double rentalFee;
    private String initialState;
    private String category;
    private int stock;
}

