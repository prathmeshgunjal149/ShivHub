package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ShopRegisterOpeningCashRequest(@NotNull LocalDate businessDate, @NotNull BigDecimal openingCash, String reason) { }
