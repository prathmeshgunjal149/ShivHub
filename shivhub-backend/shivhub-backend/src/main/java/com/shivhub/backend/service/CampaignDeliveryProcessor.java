package com.shivhub.backend.service;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.entity.MarketingDelivery;
import com.shivhub.backend.repository.MarketingDeliveryRepository;
import com.shivhub.backend.repository.MarketingCampaignRepository;

/** A separate bean makes every queued delivery use its own short database transaction. */
@Service
public class CampaignDeliveryProcessor {
    private final MarketingDeliveryRepository deliveries; private final MarketingCampaignRepository campaigns; private final EmailService email;
    public CampaignDeliveryProcessor(MarketingDeliveryRepository deliveries, MarketingCampaignRepository campaigns, EmailService email) { this.deliveries = deliveries; this.campaigns = campaigns; this.email = email; }

    @Async("campaignEmailExecutor")
    @Transactional
    public void deliverEmail(Long deliveryId) {
        MarketingDelivery delivery = deliveries.findById(deliveryId).orElse(null);
        if (delivery == null || !"PENDING".equals(delivery.getStatus())) return;
        try {
            email.sendMarketingEmail(delivery.getRecipientEmail(), delivery.getRecipientName(), delivery.getCampaign());
            delivery.setStatus("SENT"); delivery.setSentAt(LocalDateTime.now()); delivery.setMessage("Email accepted by SMTP server"); delivery.setFailureReason(null);
        } catch (Exception exception) {
            delivery.setStatus("FAILED"); delivery.setFailureReason(safe(exception)); delivery.setMessage("Email delivery failed");
        }
        deliveries.save(delivery);
        long sent = deliveries.countByCampaignIdAndStatus(delivery.getCampaign().getId(), "SENT");
        long failed = deliveries.countByCampaignIdAndStatus(delivery.getCampaign().getId(), "FAILED");
        delivery.getCampaign().setSentCount(Math.toIntExact(sent)); delivery.getCampaign().setFailedCount(Math.toIntExact(failed));
        delivery.getCampaign().setCampaignStatus(failed > 0 && sent > 0 ? "PARTIALLY_SENT" : failed > 0 ? "FAILED" : "SENT");
        campaigns.save(delivery.getCampaign());
    }
    private String safe(Exception exception) { String message = exception.getMessage(); return message == null || message.isBlank() ? "SMTP delivery failed" : message.substring(0, Math.min(message.length(), 500)); }
}
