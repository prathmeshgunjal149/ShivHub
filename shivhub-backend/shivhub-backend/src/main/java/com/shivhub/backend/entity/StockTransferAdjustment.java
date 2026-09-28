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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_transfer_adjustments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_distributor_id", nullable = false)
    private SellerDistributor sellerDistributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serial_id", nullable = false)
    private PurchaseItemSerial serial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id")
    private DistributorCreditNote creditNote;

    @Column(name = "receiving_shop_name", nullable = false, length = 150)
    private String receivingShopName;

    @Column(name = "receiving_shop_contact", length = 20)
    private String receivingShopContact;

    @Column(name = "receiving_shop_address", columnDefinition = "TEXT")
    private String receivingShopAddress;

    @Column(name = "adjustment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(name = "distributor_reference", length = 120)
    private String distributorReference;

    @Column(name = "instruction_attachment_url", length = 2000)
    private String instructionAttachmentUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "handover_confirmed_at")
    private LocalDateTime handoverConfirmedAt;

    @Column(name = "adjustment_confirmed_at")
    private LocalDateTime adjustmentConfirmedAt;

    @Column(name = "credit_note_number", length = 100)
    private String creditNoteNumber;

    @Column(name = "credit_note_date")
    private LocalDate creditNoteDate;

    @Column(name = "credit_note_attachment_url", length = 2000)
    private String creditNoteAttachmentUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
