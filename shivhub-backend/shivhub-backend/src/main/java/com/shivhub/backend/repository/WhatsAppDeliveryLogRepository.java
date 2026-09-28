package com.shivhub.backend.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.WhatsAppDeliveryLog;

public interface WhatsAppDeliveryLogRepository extends JpaRepository<WhatsAppDeliveryLog, Long> {
    Optional<WhatsAppDeliveryLog> findTopByRecipientAndEventKeyOrderByCreatedAtDesc(String recipient, String eventKey);
    Page<WhatsAppDeliveryLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
