package com.shivhub.backend.repository;

import com.shivhub.backend.entity.FinanceAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceAuditRepository extends JpaRepository<FinanceAudit, Long> {
    Page<FinanceAudit> findByFinanceSaleId(Long id, Pageable page);
}
