package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.ServiceRefundStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_refunds", indexes = @Index(name = "idx_service_refund_request_status", columnList = "service_request_id,refund_status"))
@Data
public class ServiceRefund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Column(name = "payment_transaction_id") private Long paymentTransactionId;
    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2) private BigDecimal refundAmount;
    @Enumerated(EnumType.STRING) @Column(name = "refund_method", nullable = false, length = 40) private PaymentMethod refundMethod;
    @Enumerated(EnumType.STRING) @Column(name = "refund_status", nullable = false, length = 20) private ServiceRefundStatus refundStatus = ServiceRefundStatus.INITIATED;
    @Column(name = "transaction_reference", length = 120) private String transactionReference;
    @Column(name = "initiated_at", nullable = false) private LocalDateTime initiatedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @PrePersist void create() { if (initiatedAt == null) initiatedAt = LocalDateTime.now(); }
}
