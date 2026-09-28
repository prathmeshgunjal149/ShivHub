package com.shivhub.backend.service;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.shivhub.backend.entity.ServiceNotificationLog;
import com.shivhub.backend.entity.ServiceRequest;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.ServiceNotificationLogRepository;
import com.shivhub.backend.repository.ServiceRequestRepository;
import com.shivhub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/** A notification problem is recorded but cannot undo a completed service transaction. */
@Component
@RequiredArgsConstructor
public class AfterSalesNotificationListener {
    private final ServiceRequestRepository requests;
    private final UserRepository users;
    private final OfflineBillRepository offlineBills;
    private final ServiceNotificationLogRepository logs;
    private final EmailService emailService;
    @Async("campaignEmailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyCustomer(AfterSalesNotificationEvent event) {
        ServiceRequest request = requests.findById(event.requestId()).orElse(null);
        if (request == null) return;
        Recipient recipient = recipientFor(request);
        if (recipient == null) return;
        ServiceNotificationLog log = new ServiceNotificationLog(); log.setServiceRequest(request); log.setCustomerId(recipient.customerId()); log.setNotificationType(event.type()); log.setChannel("EMAIL"); log.setRecipient(recipient.email()); log.setStatus("PENDING"); logs.save(log);
        try { emailService.sendAfterSalesStatusEmail(recipient.email(), recipient.name(), request.getRequestNumber(), request.getStatus().name(), event.remarks()); log.setStatus("SENT"); log.setSentAt(LocalDateTime.now()); }
        catch (Exception exception) { log.setStatus("FAILED"); log.setFailureReason(exception.getClass().getSimpleName() + ": " + safe(exception.getMessage())); }
        logs.save(log);
    }

    private Recipient recipientFor(ServiceRequest request) {
        if (request.getCustomerId() != null) {
            User customer = users.findById(request.getCustomerId()).orElse(null);
            if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
                return new Recipient(customer.getId(), customer.getName(), customer.getEmail().trim());
            }
        }
        if (request.getOfflineBillId() != null) {
            OfflineBill bill = offlineBills.findById(request.getOfflineBillId()).orElse(null);
            if (bill != null && bill.getCustomerEmail() != null && !bill.getCustomerEmail().isBlank()) {
                return new Recipient(null, bill.getCustomerName(), bill.getCustomerEmail().trim());
            }
        }
        return null;
    }

    private record Recipient(Long customerId, String name, String email) { }
    private String safe(String value) { return value == null ? "Email delivery failed" : value.replaceAll("(?i)(password|secret)=[^\\s]+", "$1=[redacted]"); }
}
