package com.shivhub.backend.repository;
import com.shivhub.backend.entity.DeliveryDistanceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DeliveryDistanceRuleRepository extends JpaRepository<DeliveryDistanceRule,Long> {
    List<DeliveryDistanceRule> findAllByOrderByMinimumDistanceKmAscPriorityAsc();
    List<DeliveryDistanceRule> findByProductScopeAndActiveTrueOrderByPriorityAscMinimumDistanceKmAsc(String scope);
}
