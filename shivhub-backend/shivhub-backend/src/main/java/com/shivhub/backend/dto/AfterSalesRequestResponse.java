package com.shivhub.backend.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.shivhub.backend.enums.ServicePickupType;
import com.shivhub.backend.enums.ServiceRequestStatus;
import com.shivhub.backend.enums.ServiceRequestType;
import lombok.Data;
@Data
public class AfterSalesRequestResponse {
 private Long id; private String requestNumber; private ServiceRequestType requestType; private ServiceRequestStatus status;
 private Long productId; private Long productVariantId; private String productName; private Long sellerId; private Long orderId; private Long orderItemId; private Long offlineBillId; private Long offlineBillItemId;
 private String maskedSerial; private String issueCategory; private String customerIssue; private String sellerDiagnosis; private String customerVisibleRemarks;
 private boolean warrantyEligible; private LocalDate warrantyEndDate; private boolean returnEligible; private LocalDateTime requestDate; private LocalDateTime receivedDate;
 private LocalDate expectedCompletionDate; private LocalDateTime completedDate; private LocalDateTime closedDate; private ServicePickupType pickupType; private String rejectionReason;
 private ServiceEstimateResponse estimate; private List<ServiceHistoryResponse> history; private List<ServiceAttachmentResponse> attachments;
 private List<ServiceRefundResponse> refunds;
 private boolean customerCanCancel; private boolean customerCanDecideEstimate;
}
