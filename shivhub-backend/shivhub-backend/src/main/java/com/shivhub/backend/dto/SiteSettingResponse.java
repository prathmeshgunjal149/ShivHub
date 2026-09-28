package com.shivhub.backend.dto;

public record SiteSettingResponse(
        Long id,
        String brandName,
        String legalBusinessName,
        String supportEmail,
        String supportPhone,
        String businessAddress,
        String supportHours,
        String grievanceContactName,
        String grievanceEmail,
        String websiteUrl,
        String instagramUrl,
        String facebookUrl,
        String whatsappUrl,
        String youtubeUrl,
        String enabledPaymentMethods,
        boolean demoData,
        boolean requiresReview
) {
}
