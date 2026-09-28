package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.shivhub.backend.dto.AdminCampaignRequest;
import com.shivhub.backend.dto.CampaignScheduleRequest;
import com.shivhub.backend.dto.CampaignTestRequest;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.*;

/** Admin campaign orchestration. The HTTP request snapshots and queues recipients; it never waits on SMTP. */
@Service
public class CampaignService {
    private final MarketingCampaignRepository campaigns; private final CampaignAudienceRepository audiences;
    private final MarketingDeliveryRepository deliveries; private final CampaignDeliveryProcessor processor;
    private final AdminManagementService management;

    public CampaignService(MarketingCampaignRepository campaigns, CampaignAudienceRepository audiences,
            MarketingDeliveryRepository deliveries, CampaignDeliveryProcessor processor, AdminManagementService management) {
        this.campaigns = campaigns; this.audiences = audiences; this.deliveries = deliveries;
        this.processor = processor; this.management = management;
    }

    @Transactional(readOnly = true)
    public List<MarketingCampaign> list() { return campaigns.findAll().stream().sorted(Comparator.comparing(MarketingCampaign::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList(); }
    @Transactional(readOnly = true)
    public MarketingCampaign get(Long id) { return campaigns.findById(id).orElseThrow(() -> new IllegalArgumentException("Campaign not found")); }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(AdminCampaignRequest request) {
        List<AdminManagementService.CampaignRecipient> selected = select(request);
        long validEmail = selected.stream().filter(this::isEmailEligible).map(item -> item.profile().getEmail().trim().toLowerCase(Locale.ROOT)).distinct().count();
        return row("count", selected.size(), "emailEligible", validEmail, "recipients", selected.stream().limit(20).map(candidate -> row(
                "id", candidate.profile().getId(), "name", candidate.summary().name(), "email", candidate.summary().email(),
                "type", candidate.summary().customerType(), "sellers", candidate.summary().associatedSellers())).toList());
    }

    @Transactional
    public MarketingCampaign create(AdminCampaignRequest request) {
        MarketingCampaign campaign = new MarketingCampaign(); apply(campaign, request); campaign = campaigns.save(campaign);
        snapshot(campaign, select(request));
        if (request.isSendNow()) queue(campaign.getId());
        return campaigns.save(campaign);
    }

    @Transactional
    public MarketingCampaign update(Long id, AdminCampaignRequest request) {
        MarketingCampaign campaign = get(id);
        if (!deliveries.findByCampaignIdOrderByCreatedAtDesc(id).isEmpty()) throw new IllegalStateException("A campaign cannot be edited after delivery has been queued");
        apply(campaign, request); campaign = campaigns.save(campaign); audiences.deleteByCampaignId(id); snapshot(campaign, select(request));
        return campaign;
    }

    /** Creates one immutable delivery row per customer/channel and returns immediately. */
    @Transactional
    public Map<String, Object> queue(Long id) {
        MarketingCampaign campaign = get(id);
        if (audiences.countByCampaignId(id) == 0) snapshot(campaign, selectFromStoredFilters(campaign));
        int queued = 0, skipped = 0, optedOut = 0;
        Set<String> emails = new HashSet<>();
        for (CampaignAudience audience : audiences.findByCampaignId(id)) {
            CustomerProfile profile = audience.getCustomerProfile(); String address = safeEmail(profile.getEmail());
            if (deliveries.existsByCampaignIdAndCustomerProfileIdAndChannel(id, profile.getId(), "EMAIL")) { skipped++; continue; }
            if (isOptedOut(profile)) { record(campaign, profile, "EMAIL", "OPTED_OUT", null, "Customer has opted out of marketing"); optedOut++; continue; }
            if (address == null) { record(campaign, profile, "EMAIL", "SKIPPED", null, "No valid email address"); skipped++; continue; }
            if (!emails.add(address)) { record(campaign, profile, "EMAIL", "SKIPPED", null, "Duplicate email in this campaign audience"); skipped++; continue; }
            MarketingDelivery delivery = record(campaign, profile, "EMAIL", "PENDING", null, "Queued for email delivery"); dispatchDeliveryAfterCommit(delivery.getId()); queued++;
        }
        campaign.setCampaignStatus(queued > 0 ? "QUEUED" : optedOut > 0 ? "COMPLETED" : "NO_ELIGIBLE_RECIPIENTS"); campaigns.save(campaign);
        return row("campaignId", id, "status", campaign.getCampaignStatus(), "targeted", campaign.getAudienceCount(), "queued", queued, "sent", 0, "failed", 0, "skipped", skipped, "optedOut", optedOut);
    }

    @Transactional
    public Map<String, Object> retryFailed(Long id) {
        MarketingCampaign campaign = get(id); int queued = 0;
        for (MarketingDelivery failed : deliveries.findByCampaignIdAndStatusOrderByCreatedAtDesc(id, "FAILED")) {
            failed.setStatus("PENDING"); failed.setFailureReason(null); failed.setMessage("Retry queued"); deliveries.save(failed); dispatchDeliveryAfterCommit(failed.getId()); queued++;
        }
        if (queued > 0) { campaign.setCampaignStatus("QUEUED"); campaigns.save(campaign); }
        return row("campaignId", id, "status", queued > 0 ? "QUEUED" : campaign.getCampaignStatus(), "queued", queued);
    }

    @Transactional
    public MarketingCampaign schedule(Long id, CampaignScheduleRequest request) {
        if (request.getScheduledAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Schedule time must be in the future");
        MarketingCampaign campaign = get(id); campaign.setScheduledAt(request.getScheduledAt()); campaign.setCampaignStatus("SCHEDULED"); campaign.setActive(true); return campaigns.save(campaign);
    }

    @Transactional
    public Map<String, Object> test(Long id, CampaignTestRequest request) {
        MarketingCampaign campaign = get(id); String address = safeEmail(request.getEmail());
        if (address == null) throw new IllegalArgumentException("Enter a valid test email address");
        MarketingDelivery delivery = new MarketingDelivery(); delivery.setCampaign(campaign); delivery.setRecipientEmail(address); delivery.setRecipientName("Test recipient"); delivery.setChannel("EMAIL"); delivery.setStatus("PENDING"); delivery.setMessage("Test email queued"); deliveries.save(delivery); dispatchDeliveryAfterCommit(delivery.getId());
        return row("queued", true, "status", "PENDING", "message", "Test email queued for delivery");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> report(Long id, String status, String query) {
        MarketingCampaign campaign = get(id); List<MarketingDelivery> all = deliveries.findByCampaignIdOrderByCreatedAtDesc(id);
        List<MarketingDelivery> history = all.stream().filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.getStatus())).filter(item -> query == null || query.isBlank() || (item.getRecipientEmail() + " " + item.getRecipientName()).toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT))).toList();
        Map<String, Long> counts = all.stream().collect(Collectors.groupingBy(MarketingDelivery::getStatus, LinkedHashMap::new, Collectors.counting()));
        return row("campaign", campaign, "targeted", campaign.getAudienceCount(), "selected", campaign.getAudienceCount(), "queued", counts.getOrDefault("PENDING", 0L), "sent", counts.getOrDefault("SENT", 0L), "failed", counts.getOrDefault("FAILED", 0L), "skipped", counts.getOrDefault("SKIPPED", 0L), "optedOut", counts.getOrDefault("OPTED_OUT", 0L), "byStatus", counts, "history", history);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void dispatchScheduledCampaigns() { campaigns.findByCampaignStatusAndScheduledAtLessThanEqual("SCHEDULED", LocalDateTime.now()).forEach(item -> queue(item.getId())); }

    /** Avoids an async mail worker reading a PENDING row before its queue transaction commits. */
    public void dispatchDeliveryAfterCommit(Long deliveryId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { processor.deliverEmail(deliveryId); }
            });
        } else processor.deliverEmail(deliveryId);
    }

    private void apply(MarketingCampaign campaign, AdminCampaignRequest request) {
        String type = normalize(request.getMessageType());
        if (!Set.of("FESTIVAL", "OFFER", "BIRTHDAY", "GENERAL").contains(type)) throw new IllegalArgumentException("Campaign type must be FESTIVAL, OFFER, BIRTHDAY or GENERAL");
        campaign.setType("CAMPAIGN"); campaign.setAudience(normalize(audience(request))); campaign.setTitle(request.getTitle().trim()); campaign.setMessageType(type); campaign.setFestivalName(blank(request.getFestivalName())); campaign.setEmailSubject(blank(request.getSubject())); campaign.setDescription(request.getMessageContent().trim()); campaign.setBannerUrl(blank(request.getBannerUrl())); campaign.setCouponCode(blank(request.getCouponCode())); campaign.setOfferDetails(blank(request.getOfferDetails())); campaign.setSellerId(request.getSellerIds() != null && request.getSellerIds().size() == 1 ? request.getSellerIds().get(0) : null);
        campaign.setStartsAt(request.getValidityStartsAt() == null ? LocalDateTime.now() : request.getValidityStartsAt()); campaign.setEndsAt(request.getValidityEndsAt() == null ? campaign.getStartsAt().plusDays(30) : request.getValidityEndsAt()); if (campaign.getEndsAt().isBefore(campaign.getStartsAt())) throw new IllegalArgumentException("Offer validity end must be after start");
        campaign.setChannels("EMAIL"); campaign.setTargetFilters(encode(request)); campaign.setActive(!request.isDraft()); campaign.setCampaignStatus(request.isDraft() ? "DRAFT" : request.getScheduledAt() != null ? "SCHEDULED" : "READY"); campaign.setScheduledAt(request.getScheduledAt());
    }
    private void snapshot(MarketingCampaign campaign, List<AdminManagementService.CampaignRecipient> selected) { for (AdminManagementService.CampaignRecipient candidate : selected) { CampaignAudience audience = new CampaignAudience(); audience.setCampaign(campaign); audience.setCustomerProfile(candidate.profile()); audience.setSource(candidate.summary().customerType()); audiences.save(audience); } campaign.setAudienceCount(selected.size()); campaigns.save(campaign); }
    private MarketingDelivery record(MarketingCampaign campaign, CustomerProfile profile, String channel, String status, LocalDateTime sentAt, String message) { MarketingDelivery item = new MarketingDelivery(); item.setCampaign(campaign); item.setCustomerProfileId(profile == null ? null : profile.getId()); item.setRecipientUserId(profile == null || profile.getOnlineUser() == null ? null : profile.getOnlineUser().getId()); item.setRecipientEmail(profile == null || profile.getEmail() == null ? "" : profile.getEmail().trim()); item.setRecipientName(profile == null ? "Test recipient" : profile.getName()); item.setChannel(channel); item.setStatus(status); item.setSentAt(sentAt); item.setMessage(message); return deliveries.save(item); }
    private List<AdminManagementService.CampaignRecipient> select(AdminCampaignRequest request) {
        String target = normalize(audience(request)); String legacyType = switch (target) { case "SHIVHUB_ONLINE_CUSTOMERS" -> "ONLINE"; case "SELLER_CUSTOMERS", "OFFLINE_REGISTERED_CUSTOMERS" -> "SELLER"; default -> request.getCustomerType(); }; Integer inactiveDays = request.getInactiveDays(); if ("INACTIVE_CUSTOMERS".equals(target) && (inactiveDays == null || inactiveDays < 1)) inactiveDays = 90; BigDecimal min = request.getMinimumPurchaseAmount(); if ("HIGH_VALUE_CUSTOMERS".equals(target) && min == null) min = BigDecimal.valueOf(10000);
        final Integer threshold = inactiveDays;
        return management.resolveCampaignAudience(request.getSellerIds(), legacyType, request.getCity(), threshold, min, request.getProductId(), request.getProductCategory()).stream().filter(item -> !"WALK_IN_CUSTOMERS_WITH_EMAIL".equals(target) || item.profile().getOnlineUser() == null && safeEmail(item.profile().getEmail()) != null).filter(item -> !"ACTIVE_CUSTOMERS".equals(target) || item.summary().accountEnabled()).filter(item -> !"INACTIVE_CUSTOMERS".equals(target) || !item.summary().accountEnabled() || item.summary().lastPurchaseDate() == null || item.summary().lastPurchaseDate().isBefore(LocalDateTime.now().minusDays(threshold))).toList();
    }
    private List<AdminManagementService.CampaignRecipient> selectFromStoredFilters(MarketingCampaign campaign) { Map<String,String> f = decode(campaign.getTargetFilters()); AdminCampaignRequest request = new AdminCampaignRequest(); request.setTargetAudience(f.get("targetAudience")); request.setCustomerType(f.get("customerType")); request.setSellerIds(ids(f.get("sellerIds"))); request.setCity(f.get("city")); request.setInactiveDays(integer(f.get("inactiveDays"))); request.setMinimumPurchaseAmount(decimal(f.get("minimumPurchaseAmount"))); request.setProductId(longValue(f.get("productId"))); request.setProductCategory(f.get("productCategory")); return select(request); }
    private boolean isEmailEligible(AdminManagementService.CampaignRecipient item) { return !isOptedOut(item.profile()) && safeEmail(item.profile().getEmail()) != null; } private boolean isOptedOut(CustomerProfile p) { return !p.isCommunicationConsent() || p.getOnlineUser() != null && p.getOnlineUser().isMarketingOptOut(); } private String audience(AdminCampaignRequest r) { return r.getTargetAudience() == null || r.getTargetAudience().isBlank() ? (r.getCustomerType() == null ? "ALL_CUSTOMERS" : r.getCustomerType()) : r.getTargetAudience(); }
    private String encode(AdminCampaignRequest r) { return "targetAudience=" + value(audience(r)) + ";customerType=" + value(r.getCustomerType()) + ";sellerIds=" + (r.getSellerIds() == null ? "" : r.getSellerIds().stream().map(String::valueOf).collect(Collectors.joining(","))) + ";city=" + value(r.getCity()) + ";inactiveDays=" + value(r.getInactiveDays()) + ";minimumPurchaseAmount=" + value(r.getMinimumPurchaseAmount()) + ";productId=" + value(r.getProductId()) + ";productCategory=" + value(r.getProductCategory()); }
    private Map<String,String> decode(String raw) { Map<String,String> result = new HashMap<>(); if (raw != null) for (String part : raw.split(";")) { String[] p = part.split("=", 2); if (p.length == 2) result.put(p[0], p[1]); } return result; } private String safeEmail(String email) { if (email == null) return null; String normalized = email.trim().toLowerCase(Locale.ROOT); return normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ? normalized : null; } private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); } private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); } private String value(Object value) { return value == null ? "" : String.valueOf(value); } private Integer integer(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value); } catch (NumberFormatException e) { return null; } } private BigDecimal decimal(String value) { try { return value == null || value.isBlank() ? null : new BigDecimal(value); } catch (NumberFormatException e) { return null; } } private Long longValue(String value) { try { return value == null || value.isBlank() ? null : Long.valueOf(value); } catch (NumberFormatException e) { return null; } } private List<Long> ids(String value) { return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(this::longValue).filter(Objects::nonNull).toList(); } private Map<String,Object> row(Object... values) { Map<String,Object> result = new LinkedHashMap<>(); for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]); return result; }
}
