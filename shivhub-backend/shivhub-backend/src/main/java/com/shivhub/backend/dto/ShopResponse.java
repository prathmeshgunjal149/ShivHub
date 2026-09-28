package com.shivhub.backend.dto;

/** Safe branch response; owner information is intentionally not duplicated. */
public record ShopResponse(Long id, String shopCode, String name, String city, String address, boolean active) {}
