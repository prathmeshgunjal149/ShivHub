package com.shivhub.backend.dto;

public record FaqResponse(
        Long id,
        String category,
        String question,
        String answer,
        boolean active,
        Integer displayOrder,
        boolean demoData
) {
}
