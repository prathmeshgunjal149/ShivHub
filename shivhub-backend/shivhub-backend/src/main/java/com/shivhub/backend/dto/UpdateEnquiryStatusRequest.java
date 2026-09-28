package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEnquiryStatusRequest(
        @NotBlank @Size(max = 30) String status,
        @Size(max = 5000) String internalNotes,
        @Size(max = 5000) String customerResponse
) {
}
