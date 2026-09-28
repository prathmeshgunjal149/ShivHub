package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarketingCampaignRequest {
    @NotBlank private String type;
    @NotBlank private String audience;
    @NotBlank private String title;
    private String description;
    @Size(max = 2000) private String bannerUrl;
    @Size(max = 40) private String placement;
    @Size(max = 200) private String targetFilters;
    private String couponCode;
    private BigDecimal discountPercent;
    private Long productId;
    private Long targetCustomerId;
    private boolean active = true;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean sendEmail;
}
