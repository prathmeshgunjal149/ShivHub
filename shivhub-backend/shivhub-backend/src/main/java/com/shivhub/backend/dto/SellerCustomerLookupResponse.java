package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Seller-safe customer lookup. customerProfileId is the canonical POS identity; customerId exists only for online accounts. */
public record SellerCustomerLookupResponse(
        boolean found, Long customerId, Long customerProfileId, String fullName, String mobile,
        String email, String address, String city, String district, String state, String pincode,
        LocalDate dateOfBirth, long availablePoints, long previousPurchaseCount,
        BigDecimal currentSellerDueAmount, int duplicateRecords
) { }
