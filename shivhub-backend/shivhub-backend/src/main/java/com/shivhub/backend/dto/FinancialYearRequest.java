package com.shivhub.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinancialYearRequest {
    private String yearCode;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
}
