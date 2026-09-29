package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RazorpayCredentialRequest(
        @NotBlank @Size(max = 120) String keyId,
        @NotBlank @Size(max = 1024) String keySecret) { }
