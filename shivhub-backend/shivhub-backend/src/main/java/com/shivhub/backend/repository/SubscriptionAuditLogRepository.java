package com.shivhub.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.SubscriptionAuditLog;

public interface SubscriptionAuditLogRepository extends JpaRepository<SubscriptionAuditLog, Long> {
    List<SubscriptionAuditLog> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
}
