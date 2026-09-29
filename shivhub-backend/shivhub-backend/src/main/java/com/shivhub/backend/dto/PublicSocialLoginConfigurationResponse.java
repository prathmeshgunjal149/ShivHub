package com.shivhub.backend.dto;

/** Values deliberately exposed to the login page; OAuth client/app IDs are public by design. */
public record PublicSocialLoginConfigurationResponse(
        String googleClientId,
        String facebookAppId) {
}
