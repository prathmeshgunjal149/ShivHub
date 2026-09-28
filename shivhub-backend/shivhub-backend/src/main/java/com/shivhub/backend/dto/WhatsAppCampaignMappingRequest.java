package com.shivhub.backend.dto;

import jakarta.validation.constraints.Size;

/** Non-secret values maintained by an administrator after provider approval. */
public record WhatsAppCampaignMappingRequest(
        @Size(max = 200) String templateName,
        @Size(max = 200) String campaignName,
        boolean enabled) { }
