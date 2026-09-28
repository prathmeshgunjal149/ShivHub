package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One customer record aggregated from a seller's offline bills. */
public record SellerCustomerResponse(String name, String email, String mobile,
        long billCount, BigDecimal totalSpend, LocalDateTime lastVisit) {}
