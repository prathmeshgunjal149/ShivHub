package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqRequest(
        @NotBlank @Size(max = 100) String category,
        @NotBlank @Size(max = 300) String question,
        @NotBlank @Size(max = 5000) String answer,
        Boolean active,
        Integer displayOrder,
        Boolean demoData
) {
}
