package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.SubscriptionFeature;

public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, Long> {
    Optional<SubscriptionFeature> findByFeatureCodeIgnoreCase(String featureCode);
    List<SubscriptionFeature> findByActiveTrueOrderByCategoryAscFeatureNameAsc();
}
