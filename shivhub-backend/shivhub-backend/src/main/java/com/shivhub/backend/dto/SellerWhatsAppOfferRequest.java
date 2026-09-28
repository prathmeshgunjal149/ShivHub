package com.shivhub.backend.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerWhatsAppOfferRequest(
        boolean allCustomers,
        @Size(max = 1000) List<String> recipientMobiles,
        @NotBlank @Size(max = 150) String subject,
        @NotBlank @Size(max = 3000) String message,
        @NotBlank @Size(max = 2000) String bannerUrl) { }
