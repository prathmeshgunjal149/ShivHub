package com.shivhub.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.CampaignAudience;

public interface CampaignAudienceRepository extends JpaRepository<CampaignAudience, Long> {
    List<CampaignAudience> findByCampaignId(Long campaignId);
    long countByCampaignId(Long campaignId);
    void deleteByCampaignId(Long campaignId);
}
