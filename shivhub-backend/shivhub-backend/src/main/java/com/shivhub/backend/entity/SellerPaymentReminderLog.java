package com.shivhub.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/** A small audit record that guarantees one scheduled consolidated reminder per seller per IST day. */
@Entity
@Table(name = "seller_payment_reminder_logs", uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "reminder_date"}))
@Data
public class SellerPaymentReminderLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @Column(name = "reminder_date", nullable = false) private LocalDate reminderDate;
    @Column(nullable = false, length = 16) private String status;
    private LocalDateTime sentAt;
    @Column(length = 500) private String failureReason;
}
