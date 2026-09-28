package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockTransferAdjustmentResponse {

    private Long id;
    private String status;
    private Long sellerDistributorId;
    private String distributorName;
    private Long purchaseId;
    private String purchaseInvoiceNumber;
    private Long productId;
    private String productName;
    private Long serialId;
    private String imeiOrSerial;
    private String receivingShopName;
    private String receivingShopContact;
    private String receivingShopAddress;
    private BigDecimal adjustmentAmount;
    private String distributorReference;
    private String instructionAttachmentUrl;
    private String notes;
    private Long creditNoteId;
    private String creditNoteNumber;
    private LocalDate creditNoteDate;
    private String creditNoteAttachmentUrl;
    private LocalDateTime handoverConfirmedAt;
    private LocalDateTime adjustmentConfirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
