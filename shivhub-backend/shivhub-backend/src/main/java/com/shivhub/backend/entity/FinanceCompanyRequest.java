package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_company_requests")
@Getter
@Setter
public class FinanceCompanyRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long requestedBy;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 500) private String contact;
    @Column(length = 1000) private String remarks;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    private Long reviewedBy;
    @Column(length = 1000) private String reviewReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
