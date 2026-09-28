package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.EstimateApprovalStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_estimates", uniqueConstraints = @UniqueConstraint(name = "uk_service_estimate_request", columnNames = "service_request_id"))
@Data
public class ServiceEstimate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Column(name = "inspection_charge", nullable = false, precision = 14, scale = 2) private BigDecimal inspectionCharge = BigDecimal.ZERO;
    @Column(name = "parts_amount", nullable = false, precision = 14, scale = 2) private BigDecimal partsAmount = BigDecimal.ZERO;
    @Column(name = "labour_amount", nullable = false, precision = 14, scale = 2) private BigDecimal labourAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal discount = BigDecimal.ZERO;
    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2) private BigDecimal taxableAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal cgst = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal sgst = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal igst = BigDecimal.ZERO;
    @Column(name = "grand_total", nullable = false, precision = 14, scale = 2) private BigDecimal grandTotal = BigDecimal.ZERO;
    @Column(name = "advance_amount", nullable = false, precision = 14, scale = 2) private BigDecimal advanceAmount = BigDecimal.ZERO;
    @Column(name = "remaining_amount", nullable = false, precision = 14, scale = 2) private BigDecimal remainingAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(name = "customer_approval_status", nullable = false, length = 20) private EstimateApprovalStatus customerApprovalStatus = EstimateApprovalStatus.PENDING;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create() { LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void update() { updatedAt=LocalDateTime.now(); }
}
