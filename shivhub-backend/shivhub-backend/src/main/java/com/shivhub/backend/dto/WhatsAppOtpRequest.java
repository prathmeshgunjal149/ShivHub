package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WhatsAppOtpRequest(@NotBlank @Email String email) { }
