package com.shivhub.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinanceRequest(
        @NotNull Long companyId, Long schemeId,
        @Min(1) @Max(120) int tenureMonths, @Min(0) @Max(120) int advanceMonths,
        @Size(max = 150) String loanNumber,
        @NotNull @DecimalMin("0") BigDecimal downpayment,
        @DecimalMin("0") BigDecimal processingCharges, @DecimalMin("0") BigDecimal dbdCharges,
        @DecimalMin("0") BigDecimal otherCharges, @DecimalMin("0") BigDecimal deduction,
        BigDecimal adjustment, @Size(max = 1000) String remarks,
        @NotEmpty @Size(max = 120) List<@Valid Installment> installments) {
    public record Installment(@NotBlank @Size(max = 40) String monthLabel,
                              @NotNull @DecimalMin("0") BigDecimal amount,
                              @NotNull LocalDate dueDate) { }
}
