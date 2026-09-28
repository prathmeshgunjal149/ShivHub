package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class ServiceCenterDispatchRequest {
 private Long distributorId; @NotBlank @Size(max=180) private String serviceCenterName; @NotNull private LocalDate dispatchDate;
 private String courierName; private String trackingNumber; private LocalDate expectedReturnDate; private String claimNumber; private String claimStatus;
 @DecimalMin(value="0.00") private BigDecimal claimAmount=BigDecimal.ZERO; private String remarks;
}
