package com.shivhub.backend.repository; import org.springframework.data.jpa.repository.JpaRepository; import com.shivhub.backend.entity.MarketplaceAuditLog;
public interface MarketplaceAuditLogRepository extends JpaRepository<MarketplaceAuditLog,Long>{ }
