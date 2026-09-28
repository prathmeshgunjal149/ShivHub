package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
@Data
public class ServiceEstimateRequest {
 @DecimalMin(value = "0.00") private BigDecimal inspectionCharge = BigDecimal.ZERO;
 @DecimalMin(value = "0.00") private BigDecimal labourAmount = BigDecimal.ZERO;
 @DecimalMin(value = "0.00") private BigDecimal discount = BigDecimal.ZERO;
 @DecimalMin(value = "0.00") private BigDecimal gstRate = BigDecimal.ZERO;
 @DecimalMin(value = "0.00") private BigDecimal advanceAmount = BigDecimal.ZERO;
 @Valid private List<ServicePartRequest> parts;
}
