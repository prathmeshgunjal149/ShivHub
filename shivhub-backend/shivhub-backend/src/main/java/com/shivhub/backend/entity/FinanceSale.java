package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "finance_sales", indexes = {
        @Index(name = "idx_finance_loan", columnList = "loanNumber"),
        @Index(name = "idx_finance_status", columnList = "settlementStatus")
})
@Getter
@Setter
public class FinanceSale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id", unique = true, nullable = false) private OfflineBill bill;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private FinanceCompany company;
    @Column(nullable = false, length = 150) private String companyName;
    @Column(nullable = false, length = 100) private String schemeName;
    private int tenureMonths;
    private int advanceMonths;
    @Column(length = 150) private String loanNumber;
    @Column(precision = 12, scale = 2) private BigDecimal fullSaleAmount;
    @Column(precision = 12, scale = 2) private BigDecimal downpayment;
    @Column(precision = 12, scale = 2) private BigDecimal processingCharges;
    @Column(precision = 12, scale = 2) private BigDecimal dbdCharges;
    @Column(precision = 12, scale = 2) private BigDecimal otherCharges;
    @Column(precision = 12, scale = 2) private BigDecimal deduction;
    @Column(precision = 12, scale = 2) private BigDecimal adjustment;
    @Column(precision = 12, scale = 2) private BigDecimal expectedDisbursement;
    @Column(precision = 12, scale = 2) private BigDecimal actualDisbursement = BigDecimal.ZERO;
    @Column(length = 30) private String settlementStatus = "PENDING";
    private LocalDate settlementDate;
    @Column(length = 150) private String settlementReference;
    private Long updatedBy;
    private LocalDateTime updatedAt = LocalDateTime.now();
    private boolean active = true;
    private boolean summarySent;
    private LocalDateTime summarySentAt;
    @Column(length = 1000) private String remarks;
    @Version private Long version;
}
