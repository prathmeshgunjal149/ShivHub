package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record InstantMobileBillCancelRequest(@NotBlank String reason) { }
