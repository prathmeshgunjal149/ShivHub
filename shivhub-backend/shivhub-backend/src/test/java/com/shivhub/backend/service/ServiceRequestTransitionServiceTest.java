package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.shivhub.backend.enums.ServiceRequestStatus;

class ServiceRequestTransitionServiceTest {
    private final ServiceRequestTransitionService transitions = new ServiceRequestTransitionService();
    @Test void acceptsControlledNextStep() { assertDoesNotThrow(() -> transitions.validate(ServiceRequestStatus.REQUESTED, ServiceRequestStatus.UNDER_REVIEW)); }
    @Test void rejectsSkippedWorkflowStep() { assertThrows(IllegalArgumentException.class, () -> transitions.validate(ServiceRequestStatus.REQUESTED, ServiceRequestStatus.REFUNDED)); }
}
