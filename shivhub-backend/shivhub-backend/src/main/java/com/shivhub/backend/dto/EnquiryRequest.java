package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnquiryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 20) String mobile,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 180) String subject,
        @NotBlank @Size(max = 5000) String message,
        @Size(max = 80) String orderReference
) {
}
