package com.shivhub.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.WhatsAppOtpVerification;

public interface WhatsAppOtpVerificationRepository extends JpaRepository<WhatsAppOtpVerification, Long> {
    Optional<WhatsAppOtpVerification> findTopByUserIdOrderBySentAtDesc(Long userId);
}
