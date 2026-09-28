package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import com.shivhub.backend.enums.AfterSalesNotificationType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_notification_logs", indexes = @Index(name = "idx_service_notification_request", columnList = "service_request_id"))
@Data
public class ServiceNotificationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    /** A POS walk-in return can be emailed from the bill snapshot without an online account. */
    @Column(name = "customer_id") private Long customerId;
    @Enumerated(EnumType.STRING) @Column(name = "notification_type", nullable = false, length = 40) private AfterSalesNotificationType notificationType;
    @Column(nullable = false, length = 20) private String channel;
    @Column(nullable = false, length = 300) private String recipient;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
}
