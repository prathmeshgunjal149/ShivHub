package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.shivhub.backend.entity.SellerSubscription;
import com.shivhub.backend.enums.SubscriptionStatus;

public interface SellerSubscriptionRepository extends JpaRepository<SellerSubscription, Long> {
    Optional<SellerSubscription> findBySellerId(Long sellerId);
    List<SellerSubscription> findByStatus(SubscriptionStatus status);
    @Query("select s from SellerSubscription s join fetch s.seller left join fetch s.currentPlan order by s.updatedAt desc")
    List<SellerSubscription> findAllDetailed();
}
