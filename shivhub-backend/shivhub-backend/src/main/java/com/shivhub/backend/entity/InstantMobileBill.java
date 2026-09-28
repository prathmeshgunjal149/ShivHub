package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.InstantMobileBillStatus;
import com.shivhub.backend.enums.ShopPaymentMode;

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
import lombok.Data;

/** Standalone manual cash memo. It deliberately never creates stock/GST/order records. */
@Entity
@Table(name = "instant_mobile_bills")
@Data
public class InstantMobileBill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @Column(nullable = false, unique = true, length = 48) private String billNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private InstantMobileBillStatus status = InstantMobileBillStatus.ISSUED;
    @Column(nullable = false, length = 150) private String createdByName;
    @Column(nullable = false, length = 120) private String customerName;
    @Column(nullable = false, length = 20) private String customerMobile;
    @Column(length = 180) private String customerEmail;
    @Column(columnDefinition = "TEXT") private String customerAddress;
    @Column(name = "customer_profile_id") private Long customerProfileId;
    @Column(name = "customer_id") private Long customerId;
    @Column(nullable = false) private boolean whatsappConsent;
    @Column(length = 80) private String brand;
    @Column(nullable = false, length = 160) private String model;
    @Column(length = 80) private String color;
    @Column(length = 50) private String ram;
    @Column(length = 50) private String storage;
    @Column(length = 80) private String imeiOrSerial;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discount;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ShopPaymentMode paymentMode;
    @Column(length = 120) private String paymentReference;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(columnDefinition = "TEXT") private String cancellationReason;
    @Column(length = 150) private String cancelledByName;
    private LocalDateTime cancelledAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
