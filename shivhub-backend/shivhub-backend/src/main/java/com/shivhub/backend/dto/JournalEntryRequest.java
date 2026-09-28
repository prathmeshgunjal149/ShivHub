package com.shivhub.backend.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JournalEntryRequest {
    @NotNull private LocalDate entryDate;
    @NotBlank private String sourceType;
    @NotNull private Long sourceId;
    @NotBlank private String eventType;
    private String description;
    private String createdBy;
    @NotNull private List<JournalLineRequest> lines;
}
