package com.shivhub.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/** Email delivery runs after commit; a mail outage cannot reject a support ticket or admin action. */
@Component
@RequiredArgsConstructor
public class CustomerEnquiryNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(CustomerEnquiryNotificationListener.class);
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Async("campaignEmailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notify(CustomerEnquiryNotificationEvent event) {
        try {
            if (event.type() == CustomerEnquiryNotificationEvent.Type.RECEIVED) {
                emailService.sendContactAcknowledgement(event.customerEmail(), event.customerName(), event.ticketReference());
                notifyAdmins(event);
            } else {
                emailService.sendContactActionEmail(event.customerEmail(), event.customerName(), event.ticketReference(), event.status(), event.customerResponse());
            }
        } catch (Exception exception) {
            log.warn("Contact-ticket email delivery failed for ticket {}: {}", event.ticketReference(), exception.getClass().getSimpleName());
        }
    }

    private void notifyAdmins(CustomerEnquiryNotificationEvent event) {
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;
            try {
                emailService.sendContactAdminAlert(admin.getEmail(), event.ticketReference(), event.customerName(), event.category(), event.subject());
            } catch (Exception exception) {
                log.warn("Admin contact-ticket alert failed for ticket {}: {}", event.ticketReference(), exception.getClass().getSimpleName());
            }
        }
    }
}
