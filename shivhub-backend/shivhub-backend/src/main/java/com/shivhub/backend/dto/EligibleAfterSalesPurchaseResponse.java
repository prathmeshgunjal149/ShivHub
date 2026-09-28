package com.shivhub.backend.dto;
import java.time.LocalDate;
import lombok.Data;
@Data
public class EligibleAfterSalesPurchaseResponse {
 private String source; private Long orderId; private Long orderItemId; private Long offlineBillId; private Long offlineBillItemId;
 private Long productId; private String productName; private String productImageUrl; private String invoiceNumber; private Long sellerId; private LocalDate purchaseDate;
 private Long purchaseSerialId; private String maskedSerial; private boolean warrantyActive; private LocalDate warrantyEndDate;
 private boolean returnEligible; private boolean replacementEligible; private boolean serviceEligible;
}
