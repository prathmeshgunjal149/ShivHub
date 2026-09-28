package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.LoyaltySettings;

public interface LoyaltySettingsRepository extends JpaRepository<LoyaltySettings, Long> {
    Optional<LoyaltySettings> findFirstByScopeTypeAndSellerIdAndActiveTrue(String scopeType, Long sellerId);
    Optional<LoyaltySettings> findFirstByScopeTypeAndSellerIdIsNullAndActiveTrue(String scopeType);
    List<LoyaltySettings> findByScopeTypeOrderBySellerIdAsc(String scopeType);
}
