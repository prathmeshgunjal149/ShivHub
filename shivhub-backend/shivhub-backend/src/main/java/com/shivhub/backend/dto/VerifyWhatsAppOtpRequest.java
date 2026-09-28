package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyWhatsAppOtpRequest(@NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String otp) { }
