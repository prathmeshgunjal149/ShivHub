package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.ShopRegisterAudit;
import com.shivhub.backend.entity.User;

public interface ShopRegisterAuditRepository extends JpaRepository<ShopRegisterAudit, Long> {
    List<ShopRegisterAudit> findBySellerAndEntryIdOrderByCreatedAtDesc(User seller, Long entryId);
}
