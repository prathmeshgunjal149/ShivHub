package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.shivhub.backend.entity.LoyaltyPointTransaction;

public interface LoyaltyPointTransactionRepository extends JpaRepository<LoyaltyPointTransaction, Long> {
    List<LoyaltyPointTransaction> findByCustomerProfileIdOrderByCreatedAtDesc(Long customerProfileId);
    Page<LoyaltyPointTransaction> findByCustomerProfileId(Long customerProfileId, Pageable pageable);
    Optional<LoyaltyPointTransaction> findBySourceTypeAndSourceIdAndTransactionType(String sourceType, Long sourceId, String transactionType);
}
