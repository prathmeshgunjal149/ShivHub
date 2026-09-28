package com.shivhub.backend.dto;

import java.time.LocalDateTime;

public record EnquiryResponse(
        Long id,
        String ticketReference,
        String name,
        String email,
        String mobile,
        String category,
        String subject,
        String message,
        String orderReference,
        String status,
        String internalNotes,
        String customerResponse,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
