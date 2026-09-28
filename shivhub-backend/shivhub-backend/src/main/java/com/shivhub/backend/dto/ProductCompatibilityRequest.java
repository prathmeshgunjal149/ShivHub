package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotNull;

public record ProductCompatibilityRequest(
        @NotNull Long accessoryProductId,
        String note,
        Boolean active
) {
}
