package com.shivhub.backend.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class ReplacementRequest { @NotNull private Long replacementPurchaseSerialId; @DecimalMin(value="0.00") private BigDecimal additionalPayment=BigDecimal.ZERO; @Size(max=5000) private String inspectionResult; }
