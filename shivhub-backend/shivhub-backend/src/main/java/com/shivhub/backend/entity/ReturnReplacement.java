package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.ReturnReplacementActionType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "return_replacements", uniqueConstraints = @UniqueConstraint(name = "uk_return_replacement_request", columnNames = "service_request_id"))
@Data
public class ReturnReplacement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false, length = 20) private ReturnReplacementActionType actionType;
    @Column(name = "old_purchase_serial_id") private Long oldPurchaseSerialId;
    @Column(name = "replacement_purchase_serial_id") private Long replacementPurchaseSerialId;
    @Column(name = "price_difference", nullable = false, precision = 14, scale = 2) private BigDecimal priceDifference = BigDecimal.ZERO;
    @Column(name = "additional_payment", nullable = false, precision = 14, scale = 2) private BigDecimal additionalPayment = BigDecimal.ZERO;
    @Column(name = "refundable_amount", nullable = false, precision = 14, scale = 2) private BigDecimal refundableAmount = BigDecimal.ZERO;
    @Column(name = "inspection_result", columnDefinition = "TEXT") private String inspectionResult;
    @Column(name = "completed_at") private LocalDateTime completedAt;
}
