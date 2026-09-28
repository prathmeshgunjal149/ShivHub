package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.ShopPaymentMode;
import com.shivhub.backend.enums.ShopRegisterEntryType;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Seller-only, non-GST cashbook entry. It has no relation to Product,
 * InventoryStock, Purchase, Order or any tax ledger by design.
 */
@Entity
@Table(name = "shop_register_entries")
@Data
public class ShopRegisterEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ShopRegisterEntryType entryType;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ShopPaymentMode paymentMode;

    @Column(nullable = false, length = 80) private String category;
    @Column(length = 160) private String itemName;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDateTime entryAt;
    @Column(length = 120) private String customerName;
    @Column(length = 20) private String customerMobile;
    @Column(nullable = false) private boolean whatsappConsent;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false, length = 150) private String createdByName;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void created() {
        LocalDateTime now = LocalDateTime.now();
        if (entryAt == null) entryAt = now;
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
