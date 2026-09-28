package com.shivhub.backend.dto;
import com.shivhub.backend.enums.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class ServiceStatusChangeRequest {
 @NotNull private ServiceRequestStatus status;
 @Size(max = 5000) private String remarks;
 private boolean customerVisible;
}
