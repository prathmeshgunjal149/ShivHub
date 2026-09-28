package com.shivhub.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.PlanFeature;

public interface PlanFeatureRepository extends JpaRepository<PlanFeature, Long> {
    List<PlanFeature> findByPlanId(Long planId);
    void deleteByPlanId(Long planId);
}
