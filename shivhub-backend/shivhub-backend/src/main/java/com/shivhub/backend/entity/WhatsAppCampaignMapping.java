package com.shivhub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Admin-managed, non-secret mapping from a ShivHub business event to the
 * corresponding approved WhatsApp template and live AiSensy API campaign.
 * The AiSensy API key is deliberately never stored here.
 */
@Entity
@Table(name = "whatsapp_campaign_mappings")
@Data
public class WhatsAppCampaignMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 100)
    private String eventKey;

    @Column(name = "template_name", length = 200)
    private String templateName;

    @Column(name = "campaign_name", length = 200)
    private String campaignName;

    /** Number of AiSensy body variables expected by the selected live campaign. */
    @Column(name = "template_parameter_count")
    private Integer templateParameterCount;

    /** Enabled only after Meta has approved the template and AiSensy campaign is Live. */
    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void updated() { updatedAt = LocalDateTime.now(); }
}
