package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.OpeningBalanceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "opening_balances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_opening_balance_scope", columnNames = {"seller_id", "financial_year_id", "balance_type", "party_type", "party_id", "product_id"})
})
@Data
public class OpeningBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_type", nullable = false, length = 40)
    private OpeningBalanceType balanceType;

    @Column(name = "party_type", length = 40)
    private String partyType;

    @Column(name = "party_id")
    private Long partyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column
    private Integer quantity;

    @Column(name = "unit_value", precision = 12, scale = 2)
    private BigDecimal unitValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(nullable = false)
    private boolean posted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
    }
}
