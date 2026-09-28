package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import com.shivhub.backend.enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_status_history", indexes = @Index(name = "idx_service_history_request_changed", columnList = "service_request_id,changed_at"))
@Data
public class ServiceStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 45) private ServiceRequestStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false, length = 45) private ServiceRequestStatus newStatus;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "changed_by_user_id", nullable = false) private Long changedByUserId;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
    @Column(name = "customer_visible", nullable = false) private boolean customerVisible;
    @PrePersist void onCreate() { if (changedAt == null) changedAt = LocalDateTime.now(); }
}
