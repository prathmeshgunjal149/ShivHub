package com.shivhub.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JournalLineRequest {
    @NotNull private Long ledgerAccountId;
    private String partyType;
    private Long partyId;
    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private String narration;
}
