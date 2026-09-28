package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.MarketingDelivery;

public interface MarketingDeliveryRepository extends JpaRepository<MarketingDelivery, Long> {
    List<MarketingDelivery> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);
    List<MarketingDelivery> findByCampaignIdAndStatusOrderByCreatedAtDesc(Long campaignId, String status);
    boolean existsByCampaignIdAndCustomerProfileIdAndChannel(Long campaignId, Long customerProfileId, String channel);
    long countByCampaignIdAndStatus(Long campaignId, String status);
}
