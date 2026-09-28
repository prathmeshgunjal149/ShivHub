package com.shivhub.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStockTransferAdjustmentRequest {

    @NotNull
    private Long sellerDistributorId;

    @NotNull
    private Long serialId;

    private String receivingShopName;
    private String receivingShopContact;
    private String receivingShopAddress;

    @NotNull
    private BigDecimal adjustmentAmount;

    private String distributorReference;
    private String instructionAttachmentUrl;
    private String notes;
}
