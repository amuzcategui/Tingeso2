package com.example.reportingservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej: "ACTIVE_GROUPED", "OVERDUE_CUSTOMERS", "TOP_TOOLS"
    private String reportType;

    private String fromDate; // ISO string opcional
    private String toDate;   // ISO string opcional

    private LocalDateTime generatedAt;

    // Guardamos el JSON/string del resultado (sin DTO)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;
}
