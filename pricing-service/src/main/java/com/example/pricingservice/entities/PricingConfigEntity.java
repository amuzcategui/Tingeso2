package com.example.pricingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pricing_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RF4.1 Tarifa diaria de arriendo (GENERAL)
    private double rentalFeeDaily;

    // RF4.2 Tarifa diaria de multa por atraso (GENERAL)
    private double lateFeeDaily;

    // opcional: si quieres cobrar reposición por baja, el toolValue viene desde inventory por tool (RF4.3)
}
