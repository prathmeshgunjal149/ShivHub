package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Credit note reduces the cost/payable of one distributor purchase, never revenue. */
@Entity @Table(name = "distributor_credit_notes") @Data
public class DistributorCreditNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "purchase_id", nullable = false) private Purchase purchase;
    @Column(nullable = false, length = 100) private String creditNoteNumber;
    @Column(nullable = false) private LocalDate creditNoteDate;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal gstAmount = BigDecimal.ZERO;
    @Column(length = 200) private String attachmentUrl;
    @Column(columnDefinition = "TEXT") private String reason;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void create() { if (creditNoteDate == null) creditNoteDate = LocalDate.now(); createdAt = LocalDateTime.now(); }
}
