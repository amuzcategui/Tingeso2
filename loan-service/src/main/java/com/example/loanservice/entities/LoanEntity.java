package com.example.loanservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private long id;

    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate endDate;

    private String rutCustomer;

    @ElementCollection
    @CollectionTable(name = "loan_tool_names", joinColumns = @JoinColumn(name = "loan_id"))
    @Column(name = "tool_name")
    private List<String> toolNames = new ArrayList<>();

    private double rentalFee;
    private double fine;

    @ElementCollection
    @CollectionTable(name = "loan_damaged_tools", joinColumns = @JoinColumn(name = "loan_id"))
    @Column(name = "tool_name")
    private List<String> damagedTools = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "loan_discarded_tools", joinColumns = @JoinColumn(name = "loan_id"))
    @Column(name = "tool_name")
    private List<String> discardedTools = new ArrayList<>();

    private boolean paid = false;
}
