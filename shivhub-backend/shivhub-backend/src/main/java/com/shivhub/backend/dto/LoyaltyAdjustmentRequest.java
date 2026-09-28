package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoyaltyAdjustmentRequest {
    @NotNull private Long customerProfileId;
    @NotNull private Long points;
    @NotBlank private String reason;
}
