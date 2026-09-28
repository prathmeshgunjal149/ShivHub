package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/** One declared opening balance per seller and business date. */
@Entity
@Table(name = "shop_register_opening_cash", uniqueConstraints =
        @UniqueConstraint(name = "uk_shop_register_opening_seller_date", columnNames = {"seller_id", "business_date"}))
@Data
public class ShopRegisterOpeningCash {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @Column(name = "business_date", nullable = false) private LocalDate businessDate;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal openingCash;
    @Column(nullable = false, length = 150) private String enteredByName;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void created() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
