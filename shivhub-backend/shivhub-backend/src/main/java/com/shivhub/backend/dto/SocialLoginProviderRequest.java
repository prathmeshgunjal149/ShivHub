package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A browser-visible OAuth identifier; no provider secret is accepted here. */
public record SocialLoginProviderRequest(
        @NotBlank @Size(max = 400) String clientId) {
}
