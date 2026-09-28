package com.shivhub.backend.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CaExportRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String format;
}
