package com.shivhub.backend.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_parts", indexes = @Index(name = "idx_service_part_request", columnList = "service_request_id"))
@Data
public class ServicePart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Column(name = "part_name", nullable = false, length = 180) private String partName;
    @Column(name = "part_number", length = 100) private String partNumber;
    @Column(nullable = false) private Integer quantity;
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2) private BigDecimal gstRate = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2) private BigDecimal totalAmount;
    @Column(name = "warranty_months") private Integer warrantyMonths;
}
