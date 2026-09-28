package com.shivhub.backend.dto;

import com.shivhub.backend.enums.ServicePickupType;
import com.shivhub.backend.enums.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAfterSalesRequest {
    @NotNull private ServiceRequestType requestType;
    private Long orderId;
    private Long orderItemId;
    private Long offlineBillId;
    private Long offlineBillItemId;
    private Long purchaseSerialId;
    @Size(max = 80) private String issueCategory;
    @NotBlank @Size(max = 5000) private String customerIssue;
    @NotNull private ServicePickupType pickupType;
}
