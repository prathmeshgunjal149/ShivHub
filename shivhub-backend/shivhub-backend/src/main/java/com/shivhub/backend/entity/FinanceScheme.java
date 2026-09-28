package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "finance_schemes")
@Getter
@Setter
public class FinanceScheme {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    private Long companyId;
    private int tenureMonths;
    private int advanceMonths;
    private boolean active = true;
}
