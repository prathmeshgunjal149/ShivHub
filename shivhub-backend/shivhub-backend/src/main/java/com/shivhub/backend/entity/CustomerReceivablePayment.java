package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Data;

/** Append-only customer collection. Do not update/delete financial history. */
@Entity @Table(name = "customer_receivable_payments") @Data
public class CustomerReceivablePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receivable_id", nullable = false) private CustomerReceivable receivable;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PaymentMethod paymentMethod;
    @Column(length = 100, unique = true) private String transactionReference;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) private LocalDateTime paymentDate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recorded_by") private User recordedBy;
    @PrePersist void create() { if (paymentDate == null) paymentDate = LocalDateTime.now(); }
}
