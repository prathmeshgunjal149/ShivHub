package com.shivhub.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BulkAddToCartRequest(
        @NotEmpty List<@Valid AddToCartRequest> items
) {
}
