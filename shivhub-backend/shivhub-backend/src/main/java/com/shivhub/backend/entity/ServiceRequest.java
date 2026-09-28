package com.shivhub.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.shivhub.backend.enums.ServicePickupType;
import com.shivhub.backend.enums.ServiceRequestStatus;
import com.shivhub.backend.enums.ServiceRequestType;
import com.shivhub.backend.enums.AfterSalesInspectionFinding;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

/** An additive, auditable after-sales case. Sale references intentionally remain immutable IDs. */
@Entity
@Table(name = "service_requests", indexes = {
        @Index(name = "idx_service_request_customer", columnList = "customer_id"),
        @Index(name = "idx_service_request_seller_status", columnList = "seller_id,status"),
        @Index(name = "idx_service_request_order", columnList = "order_id"),
        @Index(name = "idx_service_request_bill", columnList = "offline_bill_id"),
        @Index(name = "idx_service_request_serial", columnList = "purchase_serial_id"),
        @Index(name = "idx_service_request_created", columnList = "created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_service_request_number", columnNames = "request_number"))
@Data
public class ServiceRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "request_number", nullable = false, length = 50) private String requestNumber;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 40) private ServiceRequestType requestType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 45) private ServiceRequestStatus status = ServiceRequestStatus.REQUESTED;
    /** Null is supported for an offline walk-in return; registered customers retain their account link. */
    @Column(name = "customer_id") private Long customerId;
    @Column(name = "seller_id") private Long sellerId;
    @Column(name = "shop_id") private Long shopId;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "order_id") private Long orderId;
    @Column(name = "order_item_id") private Long orderItemId;
    @Column(name = "offline_bill_id") private Long offlineBillId;
    @Column(name = "offline_bill_item_id") private Long offlineBillItemId;
    /** Snapshot of the exact non-mobile variant sold on the original line. */
    @Column(name = "product_variant_id") private Long productVariantId;
    @Column(name = "purchase_serial_id") private Long purchaseSerialId;
    @Column(name = "imei1_snapshot", length = 30) private String imei1Snapshot;
    @Column(name = "imei2_snapshot", length = 30) private String imei2Snapshot;
    @Column(name = "serial_number_snapshot", length = 100) private String serialNumberSnapshot;
    @Column(name = "issue_category", length = 80) private String issueCategory;
    @Column(name = "customer_issue", columnDefinition = "TEXT", nullable = false) private String customerIssue;
    @Column(name = "seller_diagnosis", columnDefinition = "TEXT") private String sellerDiagnosis;
    @Enumerated(EnumType.STRING) @Column(name = "inspection_finding", length = 40) private AfterSalesInspectionFinding inspectionFinding;
    @Column(name = "receiving_condition", columnDefinition = "TEXT") private String receivingCondition;
    @Column(name = "internal_notes", columnDefinition = "TEXT") private String internalNotes;
    @Column(name = "customer_visible_remarks", columnDefinition = "TEXT") private String customerVisibleRemarks;
    @Column(name = "warranty_eligible", nullable = false) private boolean warrantyEligible;
    @Column(name = "warranty_start_date") private LocalDate warrantyStartDate;
    @Column(name = "warranty_end_date") private LocalDate warrantyEndDate;
    @Column(name = "return_eligible", nullable = false) private boolean returnEligible;
    @Column(name = "request_date", nullable = false) private LocalDateTime requestDate;
    @Column(name = "received_date") private LocalDateTime receivedDate;
    @Column(name = "expected_completion_date") private LocalDate expectedCompletionDate;
    @Column(name = "completed_date") private LocalDateTime completedDate;
    @Column(name = "closed_date") private LocalDateTime closedDate;
    @Enumerated(EnumType.STRING) @Column(name = "pickup_type", nullable = false, length = 30) private ServicePickupType pickupType;
    @Column(name = "assigned_technician_id") private Long assignedTechnicianId;
    @Column(name = "rejection_reason", columnDefinition = "TEXT") private String rejectionReason;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Version @Column(nullable = false) private Long version;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; if (requestDate == null) requestDate = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
