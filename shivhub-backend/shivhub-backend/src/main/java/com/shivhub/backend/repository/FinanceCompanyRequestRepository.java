package com.shivhub.backend.repository;

import com.shivhub.backend.entity.FinanceCompanyRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FinanceCompanyRequestRepository extends JpaRepository<FinanceCompanyRequest, Long> {
    Page<FinanceCompanyRequest> findByRequestedBy(Long user, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FinanceCompanyRequest r where r.id = :id")
    Optional<FinanceCompanyRequest> lock(@Param("id") Long id);
}
