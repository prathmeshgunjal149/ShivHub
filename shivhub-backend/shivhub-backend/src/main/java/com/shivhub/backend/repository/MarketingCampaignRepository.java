package com.shivhub.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.MarketingCampaign;
import java.util.Optional;

public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, Long> {
    List<MarketingCampaign> findByTypeOrderByCreatedAtDesc(String type);
    Optional<MarketingCampaign> findByCouponCodeIgnoreCaseAndTypeAndActiveTrue(String couponCode, String type);
    List<MarketingCampaign> findByActiveTrueAndAudienceInAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByCreatedAtDesc(
            List<String> audiences, LocalDateTime start, LocalDateTime end);
    List<MarketingCampaign> findByCampaignStatusAndScheduledAtLessThanEqual(String campaignStatus, LocalDateTime scheduledAt);
}
