package com.shivhub.backend.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.shivhub.backend.enums.ServiceRequestStatus;

/** Single source of truth for allowed after-sales state changes. */
@Service
public class ServiceRequestTransitionService {
    private final Map<ServiceRequestStatus, Set<ServiceRequestStatus>> allowed = new EnumMap<>(ServiceRequestStatus.class);
    public ServiceRequestTransitionService() {
        allow(ServiceRequestStatus.REQUESTED, ServiceRequestStatus.UNDER_REVIEW, ServiceRequestStatus.ELIGIBILITY_CHECK, ServiceRequestStatus.CANCELLED);
        allow(ServiceRequestStatus.UNDER_REVIEW, ServiceRequestStatus.APPROVED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.CANCELLED);
        allow(ServiceRequestStatus.ELIGIBILITY_CHECK, ServiceRequestStatus.APPROVED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.PRODUCT_RECEIVED, ServiceRequestStatus.CANCELLED);
        allow(ServiceRequestStatus.APPROVED, ServiceRequestStatus.PRODUCT_RECEIVED, ServiceRequestStatus.REFUND_INITIATED, ServiceRequestStatus.CANCELLED);
        allow(ServiceRequestStatus.PRODUCT_RECEIVED, ServiceRequestStatus.INSPECTION_IN_PROGRESS);
        allow(ServiceRequestStatus.INSPECTION_IN_PROGRESS, ServiceRequestStatus.WARRANTY_CONFIRMED, ServiceRequestStatus.PAID_REPAIR_APPROVAL_REQUIRED, ServiceRequestStatus.INSPECTION_PASSED, ServiceRequestStatus.INSPECTION_FAILED, ServiceRequestStatus.REJECTED);
        allow(ServiceRequestStatus.WARRANTY_CONFIRMED, ServiceRequestStatus.SENT_TO_SERVICE_CENTER, ServiceRequestStatus.REPAIR_IN_PROGRESS, ServiceRequestStatus.READY_FOR_DELIVERY);
        allow(ServiceRequestStatus.PAID_REPAIR_APPROVAL_REQUIRED, ServiceRequestStatus.ESTIMATE_APPROVED, ServiceRequestStatus.ESTIMATE_REJECTED);
        allow(ServiceRequestStatus.ESTIMATE_APPROVED, ServiceRequestStatus.REPAIR_IN_PROGRESS);
        allow(ServiceRequestStatus.ESTIMATE_REJECTED, ServiceRequestStatus.CLOSED);
        allow(ServiceRequestStatus.SENT_TO_SERVICE_CENTER, ServiceRequestStatus.REPAIR_IN_PROGRESS, ServiceRequestStatus.READY_FOR_DELIVERY);
        allow(ServiceRequestStatus.REPAIR_IN_PROGRESS, ServiceRequestStatus.READY_FOR_DELIVERY);
        allow(ServiceRequestStatus.READY_FOR_DELIVERY, ServiceRequestStatus.DELIVERED, ServiceRequestStatus.CLOSED);
        allow(ServiceRequestStatus.DELIVERED, ServiceRequestStatus.CLOSED);
        allow(ServiceRequestStatus.INSPECTION_PASSED, ServiceRequestStatus.RETURN_APPROVED, ServiceRequestStatus.REPLACEMENT_APPROVED, ServiceRequestStatus.REFUND_INITIATED);
        allow(ServiceRequestStatus.INSPECTION_FAILED, ServiceRequestStatus.REJECTED, ServiceRequestStatus.CLOSED);
        allow(ServiceRequestStatus.RETURN_APPROVED, ServiceRequestStatus.REFUND_INITIATED, ServiceRequestStatus.COMPLETED);
        allow(ServiceRequestStatus.REPLACEMENT_APPROVED, ServiceRequestStatus.REPLACED);
        allow(ServiceRequestStatus.REFUND_INITIATED, ServiceRequestStatus.REFUNDED);
        allow(ServiceRequestStatus.REFUNDED, ServiceRequestStatus.COMPLETED);
        allow(ServiceRequestStatus.REPLACED, ServiceRequestStatus.COMPLETED);
    }
    private void allow(ServiceRequestStatus from, ServiceRequestStatus... targets) { allowed.put(from, EnumSet.of(targets[0], targets)); }
    public void validate(ServiceRequestStatus from, ServiceRequestStatus to) {
        if (from == to || !allowed.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("Invalid after-sales status transition: " + from + " to " + to);
        }
    }
}
