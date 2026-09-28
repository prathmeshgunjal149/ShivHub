package com.shivhub.backend.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.SellerPaymentReminderLog;
import com.shivhub.backend.entity.User;

public interface SellerPaymentReminderLogRepository extends JpaRepository<SellerPaymentReminderLog, Long> {
    Optional<SellerPaymentReminderLog> findBySellerAndReminderDate(User seller, LocalDate reminderDate);
}
