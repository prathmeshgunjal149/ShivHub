package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionAdminActionRequest(Integer extensionDays, Boolean active, Boolean autoRenewEnabled,
        @NotBlank String reason) { }
