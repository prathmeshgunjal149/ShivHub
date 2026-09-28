package com.shivhub.backend.dto;
import java.time.LocalDateTime;
public record MarketplaceOwnerSummary(Long customerId, String name, String email, String mobile,
        LocalDateTime registeredAt, long totalListings, long activeListings, long soldListings, long enquiriesReceived) { }
