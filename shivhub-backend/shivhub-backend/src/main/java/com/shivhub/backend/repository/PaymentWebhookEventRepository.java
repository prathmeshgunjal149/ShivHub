package com.shivhub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.PaymentWebhookEvent;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    boolean existsByGatewayEventId(String gatewayEventId);
}
