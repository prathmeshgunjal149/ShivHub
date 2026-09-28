package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request used only by Admin to connect one distributor and brand to one seller. */
@Data
public class AdminDistributorAssignmentRequest {
    @NotNull(message = "Seller is required")
    private Long sellerId;
    @NotNull(message = "Distributor is required")
    private Long distributorId;
    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;
    private String assignedBrands;
    private String paymentTerms;
    private Integer creditPeriodDays;
    private BigDecimal creditLimit;
    private BigDecimal openingBalance;
    private LocalDate openingBalanceDate;
    private String openingBalanceType;
    private String preferredPaymentMethod;
    private String assignedSalesperson;
    private String sellerNotes;
    private Boolean active = true;
}
