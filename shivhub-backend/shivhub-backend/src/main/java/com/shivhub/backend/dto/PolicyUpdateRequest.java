package com.shivhub.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PolicyUpdateRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 300) String description,
        @NotBlank @Size(max = 30000) String content,
        LocalDate effectiveDate,
        Boolean demoData,
        Boolean requiresReview,
        Boolean confirmProductionReady
) {
}
