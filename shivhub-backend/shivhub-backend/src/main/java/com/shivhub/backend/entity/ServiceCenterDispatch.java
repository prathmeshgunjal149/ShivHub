package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_center_dispatches", uniqueConstraints = @UniqueConstraint(name = "uk_service_dispatch_request", columnNames = "service_request_id"))
@Data
public class ServiceCenterDispatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Column(name = "distributor_id") private Long distributorId;
    @Column(name = "service_center_name", nullable = false, length = 180) private String serviceCenterName;
    @Column(name = "dispatch_date", nullable = false) private LocalDate dispatchDate;
    @Column(name = "courier_name", length = 100) private String courierName;
    @Column(name = "tracking_number", length = 120) private String trackingNumber;
    @Column(name = "expected_return_date") private LocalDate expectedReturnDate;
    @Column(name = "received_back_date") private LocalDate receivedBackDate;
    @Column(name = "claim_number", length = 100) private String claimNumber;
    @Column(name = "claim_status", length = 50) private String claimStatus;
    @Column(name = "claim_amount", precision = 14, scale = 2) private BigDecimal claimAmount = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "credit_note_reference", length = 120) private String creditNoteReference;
}
