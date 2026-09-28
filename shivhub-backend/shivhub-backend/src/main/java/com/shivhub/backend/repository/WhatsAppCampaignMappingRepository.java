package com.shivhub.backend.repository;

import com.shivhub.backend.entity.WhatsAppCampaignMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppCampaignMappingRepository extends JpaRepository<WhatsAppCampaignMapping, Long> {
    Optional<WhatsAppCampaignMapping> findByEventKey(String eventKey);
}
