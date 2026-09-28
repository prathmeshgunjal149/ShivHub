package com.shivhub.backend.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoyaltySettingsRequest {
    private Long sellerId;
    @NotNull @DecimalMin("0.00") private BigDecimal minimumPurchaseAmount;
    @NotNull @Min(1) private Long pointsPerPurchaseUnit;
    @NotNull @DecimalMin("0.01") private BigDecimal purchaseUnitInRupees;
    @NotNull @DecimalMin("0.01") private BigDecimal pointValueInRupees;
    @NotNull @Min(0) private Long maximumPointsPerSale;
    private boolean active = true;
}
