package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Condition ledger for returns; it never inflates sellable product stock before inspection. */
@Entity
@Table(name = "after_sales_stock_movements",
        indexes = @Index(name = "idx_after_sales_stock_request", columnList = "service_request_id,created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_after_sales_variant_disposition", columnNames = {"service_request_id", "product_variant_id"}))
@Data
public class AfterSalesStockMovement {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_request_id",nullable=false) private ServiceRequest serviceRequest;
 @Column(name="purchase_serial_id") private Long purchaseSerialId;
 @Column(name="product_variant_id") private Long productVariantId;
 @Column(name="quantity") private Integer quantity;
 @Column(name="stock_restored", nullable=false) private boolean stockRestored;
 @Column(name="from_status",length=40) private String fromStatus;
 @Column(name="to_status",nullable=false,length=40) private String toStatus;
 @Column(name="changed_by_user_id",nullable=false) private Long changedByUserId;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(columnDefinition="TEXT") private String remarks;
 @PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
