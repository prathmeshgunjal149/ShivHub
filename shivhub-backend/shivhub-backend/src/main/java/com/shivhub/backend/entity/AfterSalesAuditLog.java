package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "after_sales_audit_logs", indexes = @Index(name = "idx_after_sales_audit_request", columnList = "service_request_id,created_at"))
@Data
public class AfterSalesAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Column(name = "actor_user_id", nullable = false) private Long actorUserId;
    @Column(nullable = false, length = 60) private String action;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
