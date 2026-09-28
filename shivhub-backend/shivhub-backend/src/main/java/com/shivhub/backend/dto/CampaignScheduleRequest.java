package com.shivhub.backend.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampaignScheduleRequest {
    @NotNull private LocalDateTime scheduledAt;
}
