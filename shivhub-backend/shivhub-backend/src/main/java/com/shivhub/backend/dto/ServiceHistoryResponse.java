package com.shivhub.backend.dto;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.ServiceRequestStatus;
import lombok.Data;
@Data public class ServiceHistoryResponse { private ServiceRequestStatus previousStatus; private ServiceRequestStatus newStatus; private String remarks; private LocalDateTime changedAt; private boolean customerVisible; }
