package com.shivhub.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "seller_distributors",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"seller_id", "distributor_id", "brand"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDistributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // SELLER
    // =========================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    private User seller;

    // =========================
    // DISTRIBUTOR
    // =========================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    // =========================
    // BRAND
    // =========================
    @Column(nullable = false, length = 100)
    private String brand;

    @Column(name = "assigned_brands", length = 500)
    private String assignedBrands;

    @Column(name = "payment_terms", length = 80)
    private String paymentTerms;

    @Column(name = "credit_period_days")
    private Integer creditPeriodDays;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "opening_balance", precision = 12, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;

    @Column(name = "opening_balance_type", length = 40)
    private String openingBalanceType;

    @Column(name = "preferred_payment_method", length = 80)
    private String preferredPaymentMethod;

    @Column(name = "assigned_salesperson", length = 120)
    private String assignedSalesperson;

    @Column(name = "seller_notes", columnDefinition = "TEXT")
    private String sellerNotes;

    // =========================
    // ACTIVE / INACTIVE
    // =========================
    @Column(nullable = false)
    private Boolean active = true;

    // =========================
    // TIMESTAMPS
    // =========================
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // =========================
    // BEFORE INSERT
    // =========================
    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (active == null) {
            active = true;
        }
    }

    // =========================
    // BEFORE UPDATE
    // =========================
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
