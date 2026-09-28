package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BirthdayGreetingSettingRequest {
    @NotBlank private String emailSubject;
    @NotBlank private String messageContent;
    private String couponCode;
    private String bannerUrl;
    private boolean active;
}
