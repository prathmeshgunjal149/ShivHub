package com.shivhub.backend.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Non-secret values maintained by an administrator after provider approval. */
public record WhatsAppCampaignMappingRequest(
        @Size(max = 200) String templateName,
        @Size(max = 200) String campaignName,
        @Min(0) @Max(20) Integer templateParameterCount,
        boolean enabled) { }
