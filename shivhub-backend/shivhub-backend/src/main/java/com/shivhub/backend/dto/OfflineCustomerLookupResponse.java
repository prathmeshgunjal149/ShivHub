package com.shivhub.backend.dto;

public record OfflineCustomerLookupResponse(
        Long customerId,
        String name,
        String mobile,
        String email,
        String address,
        String gstin
) {}
