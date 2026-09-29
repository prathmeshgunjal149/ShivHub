package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SmtpCredentialRequest(
        @NotBlank @Email @Size(max = 320) String username,
        @NotBlank @Size(max = 1024) String password) { }
