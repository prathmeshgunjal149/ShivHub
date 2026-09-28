package com.shivhub.backend.dto;

/** Safe staff option displayed in the POS salesperson selector. */
public record SalespersonResponse(Long id, String name, String mobile, String shopName, String accessRole) {}
