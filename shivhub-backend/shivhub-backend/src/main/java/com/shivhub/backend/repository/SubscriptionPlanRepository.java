package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.SubscriptionPlan;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);
    List<SubscriptionPlan> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<SubscriptionPlan> findAllByOrderByDisplayOrderAscNameAsc();
}
