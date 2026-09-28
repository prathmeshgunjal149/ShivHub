package com.shivhub.backend.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Seller offer campaign. Email is sent to selected customers; WhatsApp is additionally sent only to profiles with explicit consent. */
public record SellerEmailCampaignRequest(
        @NotEmpty List<String> recipientEmails,
        @NotBlank @Size(max = 150) String subject,
        @NotBlank @Size(max = 3000) String message,
        @Size(max = 2000) String bannerUrl
) {}
