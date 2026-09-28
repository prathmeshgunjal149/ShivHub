package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Return intake created at a seller's POS counter after the physical unit is handed back. */
@Data
public class SellerOfflineReturnRequest {
    @NotNull private Long offlineBillId;
    @NotNull private Long offlineBillItemId;
    private Long purchaseSerialId;
    @NotBlank @Size(max = 80) private String returnReason;
    @NotBlank @Size(max = 5000) private String customerIssue;
    @NotBlank @Size(max = 5000) private String receivingCondition;
}
