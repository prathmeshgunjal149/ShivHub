package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.PaymentContext;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

/** Immutable-audit oriented payment record for gateway and manual collections. */
@Entity
@Table(name = "payment_transactions",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_payment_reference", columnNames = "payment_reference"),
           @UniqueConstraint(name = "uk_razorpay_order", columnNames = "razorpay_order_id"),
           @UniqueConstraint(name = "uk_razorpay_payment", columnNames = "razorpay_payment_id")
       })
@Data
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_reference", nullable = false, length = 80) private String paymentReference;
    @Column(name = "razorpay_order_id", length = 80) private String razorpayOrderId;
    @Column(name = "razorpay_payment_id", length = 80) private String razorpayPaymentId;
    @Column(name = "razorpay_signature", length = 255) private String razorpaySignature;
    @Column(name = "razorpay_refund_id", length = 80) private String razorpayRefundId;
    @Column(name = "razorpay_payment_link_id", unique = true, length = 80) private String razorpayPaymentLinkId;
    @Column(name = "razorpay_payment_link_url", length = 500) private String razorpayPaymentLinkUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private PaymentContext paymentContext;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
    @Column(nullable = false, length = 3) private String currency = "INR";
    @Column(name = "order_id") private Long orderId;
    @Column(name = "offline_bill_id") private Long offlineBillId;
    @Column(name = "customer_receivable_id") private Long customerReceivableId;
    @Column(name = "purchase_id") private Long purchaseId;
    @Column(name = "customer_id") private Long customerId;
    @Column(name = "seller_id") private Long sellerId;
    @Column(name = "payer_name", length = 160) private String payerName;
    @Column(name = "payer_mobile", length = 20) private String payerMobile;
    @Column(name = "transaction_reference", length = 120) private String transactionReference;
    @Column(name = "failure_code", length = 80) private String failureCode;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "settled_at") private LocalDateTime settledAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void createTimestamps() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}
