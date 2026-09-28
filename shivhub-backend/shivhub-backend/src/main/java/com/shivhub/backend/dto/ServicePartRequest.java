package com.shivhub.backend.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ServicePartRequest {
 @NotBlank @Size(max = 180) private String partName;
 @Size(max = 100) private String partNumber;
 @NotNull @Min(1) private Integer quantity;
 @NotNull @DecimalMin(value = "0.00") private BigDecimal unitPrice;
 @NotNull @DecimalMin(value = "0.00") private BigDecimal gstRate;
 @Min(0) private Integer warrantyMonths;
}
