package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Stores only delivery metadata for a gateway webhook.  The raw gateway
 * payload is deliberately not persisted because it can contain unnecessary
 * payment and customer metadata.  The event id also provides a durable
 * idempotency key when Razorpay retries delivery.
 */
@Entity
@Table(name = "payment_webhook_events")
@Data
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway_event_id", nullable = false, unique = true, length = 120)
    private String gatewayEventId;

    @Column(name = "event_type", length = 80)
    private String eventType;

    @Column(name = "razorpay_order_id", length = 80)
    private String razorpayOrderId;

    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;

    @Column(nullable = false, length = 40)
    private String processingResult;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
