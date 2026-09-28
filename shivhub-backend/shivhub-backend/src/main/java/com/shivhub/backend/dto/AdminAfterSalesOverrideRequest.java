package com.shivhub.backend.dto;
import com.shivhub.backend.enums.ServiceRequestStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class AdminAfterSalesOverrideRequest { @NotNull private ServiceRequestStatus status; @NotBlank @Size(max=5000) private String reason; private boolean customerVisible; }
