package com.shivhub.backend.dto;

/** Safe authenticated-customer loyalty balance; no customer identifier is accepted from the client. */
public record CustomerLoyaltySummaryResponse(
        long availablePoints,
        long lifetimeEarnedPoints,
        long lifetimeRedeemedPoints,
        long expiringPoints
) { }
