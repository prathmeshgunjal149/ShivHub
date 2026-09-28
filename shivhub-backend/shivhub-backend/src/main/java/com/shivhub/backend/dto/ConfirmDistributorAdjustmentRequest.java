package com.shivhub.backend.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ConfirmDistributorAdjustmentRequest {

    private String creditNoteNumber;
    private LocalDate creditNoteDate;
    private String attachmentUrl;
    private String remarks;
}
