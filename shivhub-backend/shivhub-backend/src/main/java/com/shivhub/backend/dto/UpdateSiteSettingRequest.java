package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSiteSettingRequest(
        @NotBlank @Size(max = 120) String brandName,
        @NotBlank @Size(max = 200) String legalBusinessName,
        @NotBlank @Email @Size(max = 180) String supportEmail,
        @Size(max = 80) String supportPhone,
        @Size(max = 4000) String businessAddress,
        @Size(max = 180) String supportHours,
        @Size(max = 160) String grievanceContactName,
        @Email @Size(max = 180) String grievanceEmail,
        @Size(max = 2000) String websiteUrl,
        @Size(max = 2000) String instagramUrl,
        @Size(max = 2000) String facebookUrl,
        @Size(max = 2000) String whatsappUrl,
        @Size(max = 2000) String youtubeUrl,
        @Size(max = 500) String enabledPaymentMethods,
        Boolean demoData,
        Boolean requiresReview
) {
}
