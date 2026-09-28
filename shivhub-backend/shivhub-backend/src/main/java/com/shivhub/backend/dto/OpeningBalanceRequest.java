package com.shivhub.backend.dto;

import java.math.BigDecimal;

import com.shivhub.backend.enums.OpeningBalanceType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpeningBalanceRequest {
    @NotNull private Long financialYearId;
    @NotNull private OpeningBalanceType balanceType;
    private String partyType;
    private Long partyId;
    private Long productId;
    private BigDecimal amount = BigDecimal.ZERO;
    private Integer quantity;
    private BigDecimal unitValue;
    private String notes;
}
