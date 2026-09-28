package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Provider-agnostic delivery audit; message text and credentials are deliberately never stored. */
@Entity
@Table(name = "whatsapp_delivery_logs", indexes = {
        @Index(name = "idx_whatsapp_delivery_key", columnList = "recipient,event_key"),
        @Index(name = "idx_whatsapp_delivery_status", columnList = "status,created_at") })
@Data
public class WhatsAppDeliveryLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 30) private String recipient;
    @Column(name = "event_key", nullable = false, length = 128) private String eventKey;
    @Column(name = "event_type", length = 80) private String eventType;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "provider_status", length = 60) private String providerStatus;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created() { createdAt = LocalDateTime.now(); }
}
