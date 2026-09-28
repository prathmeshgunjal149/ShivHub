package com.shivhub.backend.dto;
import java.time.LocalDateTime;
public record MarketplaceAdminInquiryResponse(Long id, Long listingId, String listingTitle,
        Long ownerId, String ownerName, String ownerMobile, Long buyerId, String buyerName,
        String buyerMobile, String buyerEmail, String message, String status, LocalDateTime createdAt) { }
