package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Small, role-safe search result. It deliberately does not expose entities or internal pricing. */
public record SearchSuggestionItem(
        Long id,
        String type,
        String label,
        String secondaryLabel,
        String imageUrl,
        BigDecimal price,
        String categoryName,
        String subcategoryName,
        String route,
        Map<String, String> metadata
) {}
