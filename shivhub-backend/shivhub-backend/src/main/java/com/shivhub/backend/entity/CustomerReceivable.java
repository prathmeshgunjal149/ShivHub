package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Immutable-sale receivable header. Payments are recorded in CustomerReceivablePayment. */
@Entity @Table(name = "customer_receivables") @Data
public class CustomerReceivable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @Column(nullable = false, length = 150) private String customerName;
    @Column(length = 30) private String customerMobile;
    @Column(length = 150) private String customerEmail;
    @Column(length = 100) private String invoiceNumber;
    @Column(columnDefinition = "TEXT") private String productDetails;
    @Column(length = 120) private String imei;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal saleAmount = BigDecimal.ZERO;
    @Column(nullable = false) private LocalDate saleDate;
    private LocalDate dueDate;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void create() { if (saleDate == null) saleDate = LocalDate.now(); createdAt = LocalDateTime.now(); }
}
