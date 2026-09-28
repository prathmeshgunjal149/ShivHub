package com.shivhub.backend.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record DeliveryRuleDto(Long id, @NotBlank @Size(max=120) String name, String productScope,
    @NotNull @DecimalMin("0") BigDecimal minimumDistanceKm, BigDecimal maximumDistanceKm,
    @NotBlank @Size(max=500) String estimatedDeliveryText, Integer estimatedMinutes, boolean active,
    int priority, @DecimalMin("0") BigDecimal deliveryCharge, boolean serviceAvailable) { }
