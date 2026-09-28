package com.shivhub.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Seller settings update. Email cannot be changed here because it is also the login identity. */
public record UpdateSellerProfileRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$") String mobile,
        @Size(max = 150) String businessName,
        @Size(max = 150) String legalBusinessName,
        @Size(max = 2000) String shopLogoUrl,
        @Size(max = 15) String gstin,
        @Size(max = 3000) String businessAddress,
        String alternateMobile,
        String businessCity,
        String businessDistrict,
        String businessState,
        String businessPincode,
        @Size(max = 2000) String googleMapsUrl,
        @Size(max = 2000) String websiteUrl,
        @Size(max = 2000) String instagramUrl,
        @Size(max = 2000) String facebookUrl,
        @Size(max = 2000) String whatsappUrl,
        @Size(max = 2000) String youtubeUrl,
        LocalDate shopOpeningDate,
        String shopType,
        String panNumber,
        String businessRegistrationNumber,
        String gstRegistrationType,
        String stateCode,
        @Size(max = 3000) String taxSettings,
        String gstRates,
        @Size(max = 3000) String hsnSacSettings,
        String invoicePrefix,
        @Size(max = 3000) String invoiceTerms,
        String warrantyPeriod,
        String warrantyType,
        @Size(max = 3000) String returnPolicy,
        @Size(max = 3000) String replacementPolicy,
        @Size(max = 3000) String exchangePolicy,
        @Size(max = 3000) String refundPolicy,
        @Size(max = 3000) String warrantyTerms,
        boolean invoiceShowAddress,
        boolean invoiceShowMobile,
        boolean invoiceShowGstin,
        boolean invoiceShowCustomerDetails,
        boolean invoiceShowNotes,
        boolean invoiceShowWebsite,
        boolean invoiceShowSocialLinks
) {}
