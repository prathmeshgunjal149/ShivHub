package com.shivhub.backend.dto;

/** Admin view of public social sign-in configuration. */
public record AdminSocialLoginConfigurationResponse(
        boolean googleConfigured,
        String googleClientId,
        String googleSource,
        boolean facebookConfigured,
        String facebookAppId,
        String facebookSource) {
}
