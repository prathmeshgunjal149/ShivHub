package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Input for a seller creating or editing a branch. */
public record ShopRequest(
        @NotBlank(message = "Shop code is required") String shopCode,
        @NotBlank(message = "Shop name is required") String name,
        String city,
        String address
) {}
