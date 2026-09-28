package com.shivhub.backend.dto;

import jakarta.validation.constraints.*;

public record FinanceSchemeInput(@NotBlank @Size(max = 100) String name,
                                 Long companyId,
                                 @Min(1) @Max(120) int tenureMonths,
                                 @Min(0) @Max(120) int advanceMonths,
                                 boolean active) { }
