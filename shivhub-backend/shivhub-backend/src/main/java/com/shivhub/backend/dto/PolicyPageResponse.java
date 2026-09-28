package com.shivhub.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PolicyPageResponse(
        Long id,
        String slug,
        String title,
        String description,
        String content,
        String draftContent,
        LocalDate effectiveDate,
        boolean published,
        boolean demoData,
        boolean requiresReview,
        LocalDateTime lastPublishedAt,
        Long lastPublishedByAdminId,
        LocalDateTime lastEditedAt,
        Long lastEditedByAdminId
) {
}
