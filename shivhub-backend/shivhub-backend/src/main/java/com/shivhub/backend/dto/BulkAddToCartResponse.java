package com.shivhub.backend.dto;

import java.util.List;

public record BulkAddToCartResponse(
        boolean success,
        List<String> errors
) {
}
