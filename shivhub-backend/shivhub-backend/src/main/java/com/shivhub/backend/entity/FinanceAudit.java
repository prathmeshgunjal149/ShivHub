package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_audit")
@Getter
@Setter
public class FinanceAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long financeSaleId;
    private Long actorId;
    @Column(length = 40) private String action;
    @Column(length = 1000) private String details;
    private LocalDateTime createdAt = LocalDateTime.now();
}
