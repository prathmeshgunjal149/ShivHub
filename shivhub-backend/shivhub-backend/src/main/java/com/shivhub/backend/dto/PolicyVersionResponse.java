package com.shivhub.backend.dto;

import java.time.LocalDateTime;

public record PolicyVersionResponse(
        Long id,
        String title,
        String content,
        String action,
        Long adminId,
        LocalDateTime createdAt
) {
}
