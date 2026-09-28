package com.shivhub.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceSettlementRequest(@NotNull @DecimalMin("0") BigDecimal amount,
                                      @NotNull LocalDate date,
                                      @NotBlank @Size(max = 150) String reference,
                                      @NotBlank String status,
                                      @Size(max = 700) String reason) { }
