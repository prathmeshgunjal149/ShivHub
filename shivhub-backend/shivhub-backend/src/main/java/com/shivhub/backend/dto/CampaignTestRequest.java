package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CampaignTestRequest {
    @NotBlank private String email;
}
