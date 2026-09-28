package com.shivhub.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.BirthdayGreetingSettingRequest;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.*;

/** Daily India-timezone birthday sender. A fresh campaign per year makes duplicate prevention auditable. */
@Service
public class BirthdayGreetingService {
    private final BirthdayGreetingSettingRepository settings; private final CustomerProfileRepository profiles;
    private final MarketingCampaignRepository campaigns; private final MarketingDeliveryRepository deliveries;
    private final CampaignService campaignService; private final UserRepository users;
    public BirthdayGreetingService(BirthdayGreetingSettingRepository settings, CustomerProfileRepository profiles, MarketingCampaignRepository campaigns, MarketingDeliveryRepository deliveries, CampaignService campaignService, UserRepository users) { this.settings=settings; this.profiles=profiles; this.campaigns=campaigns; this.deliveries=deliveries; this.campaignService=campaignService; this.users=users; }

    /** The admin page must be able to reopen a disabled configuration without making it active. */
    @Transactional(readOnly = true)
    public BirthdayGreetingSetting current() { return settings.findFirstByOrderByIdDesc().orElse(null); }
    @Transactional
    public BirthdayGreetingSetting save(BirthdayGreetingSettingRequest request) {
        // Keep the configuration singleton in effect. In particular, saving it as disabled
        // must not leave a historic active template behind for the scheduler to use.
        settings.findAll().forEach(item -> item.setActive(false));
        BirthdayGreetingSetting item = current();
        if (item == null) item = new BirthdayGreetingSetting();
        item.setEmailSubject(request.getEmailSubject().trim()); item.setMessageContent(request.getMessageContent().trim()); item.setCouponCode(blank(request.getCouponCode())); item.setBannerUrl(blank(request.getBannerUrl())); item.setActive(request.isActive()); return settings.save(item);
    }

    @Scheduled(cron = "${shivhub.birthday.cron:0 0 10 * * *}", zone = "${shivhub.birthday.zone:Asia/Kolkata}")
    @Transactional
    public Map<String, Object> sendToday() {
        BirthdayGreetingSetting setting = settings.findFirstByActiveTrueOrderByIdDesc().orElse(null);
        if (setting == null) return results(0, 0, 0, 0, "DISABLED");
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")); MarketingCampaign campaign = yearlyCampaign(setting, today.getYear());
        List<CustomerProfile> knownProfiles = new ArrayList<>(profiles.findAll());
        Map<Long, CustomerProfile> profilesByOnlineUserId = new HashMap<>();
        for (CustomerProfile profile : knownProfiles) if (profile.getOnlineUser() != null && profile.getOnlineUser().getId() != null) profilesByOnlineUserId.put(profile.getOnlineUser().getId(), profile);
        // Existing online users are included even if the one-time profile backfill was not run yet.
        for (User user : users.findByRole(Role.CUSTOMER)) {
            if (!user.isEnabled() || !birthdayOn(user.getDateOfBirth(), today) || profilesByOnlineUserId.containsKey(user.getId())) continue;
            CustomerProfile profile = new CustomerProfile(); profile.setOnlineUser(user); profile.setName(user.getName()); profile.setMobile(user.getMobile()); profile.setEmail(user.getEmail()); profile.setDateOfBirth(user.getDateOfBirth()); profile.setCommunicationConsent(!user.isMarketingOptOut());
            profile = profiles.save(profile); knownProfiles.add(profile); profilesByOnlineUserId.put(user.getId(), profile);
        }
        Set<String> emailAlreadyRecorded = new HashSet<>();
        for (MarketingDelivery delivery : deliveries.findByCampaignIdOrderByCreatedAtDesc(campaign.getId())) {
            String recorded = validEmail(delivery.getRecipientEmail());
            if (recorded != null && ("PENDING".equals(delivery.getStatus()) || "SENT".equals(delivery.getStatus()) || "FAILED".equals(delivery.getStatus()))) emailAlreadyRecorded.add(recorded);
        }
        int targeted = 0, queued = 0, skipped = 0, optedOut = 0;
        for (CustomerProfile profile : knownProfiles) {
            if (!birthdayOn(profile.getDateOfBirth(), today)) continue;
            targeted++;
            if (deliveries.existsByCampaignIdAndCustomerProfileIdAndChannel(campaign.getId(), profile.getId(), "EMAIL")) continue;
            String email = validEmail(profile.getEmail());
            MarketingDelivery delivery = new MarketingDelivery(); delivery.setCampaign(campaign); delivery.setCustomerProfileId(profile.getId()); delivery.setRecipientUserId(profile.getOnlineUser() == null ? null : profile.getOnlineUser().getId()); delivery.setRecipientEmail(email == null ? "" : email); delivery.setRecipientName(profile.getName()); delivery.setChannel("EMAIL");
            if (!profile.isCommunicationConsent() || profile.getOnlineUser() != null && profile.getOnlineUser().isMarketingOptOut()) { delivery.setStatus("OPTED_OUT"); delivery.setMessage("Customer has opted out of marketing"); optedOut++; }
            else if (email == null) { delivery.setStatus("SKIPPED"); delivery.setMessage("No valid email address"); skipped++; }
            else if (!emailAlreadyRecorded.add(email)) { delivery.setStatus("SKIPPED"); delivery.setMessage("Duplicate email already recorded for this birthday campaign"); skipped++; }
            else { delivery.setStatus("PENDING"); delivery.setMessage("Birthday greeting queued"); queued++; }
            deliveries.save(delivery); if ("PENDING".equals(delivery.getStatus())) campaignService.dispatchDeliveryAfterCommit(delivery.getId());
        }
        campaign.setAudienceCount(targeted); campaign.setCampaignStatus(targeted == 0 ? "NO_ELIGIBLE_RECIPIENTS" : "QUEUED"); campaigns.save(campaign);
        return results(targeted, queued, skipped, optedOut, campaign.getCampaignStatus());
    }

    private MarketingCampaign yearlyCampaign(BirthdayGreetingSetting setting, int year) {
        String title = "Birthday greetings " + year;
        MarketingCampaign campaign = campaigns.findAll().stream().filter(item -> title.equals(item.getTitle())).findFirst().orElseGet(() -> { MarketingCampaign c = new MarketingCampaign(); c.setType("CAMPAIGN"); c.setAudience("BIRTHDAY"); c.setTitle(title); c.setMessageType("BIRTHDAY"); c.setChannels("EMAIL"); c.setActive(true); c.setStartsAt(LocalDateTime.now()); c.setEndsAt(LocalDateTime.now().plusYears(1)); return c; });
        // A template edit should apply to birthdays that have not yet occurred this year.
        campaign.setEmailSubject(setting.getEmailSubject()); campaign.setDescription(setting.getMessageContent()); campaign.setCouponCode(setting.getCouponCode()); campaign.setBannerUrl(setting.getBannerUrl());
        if (campaign.getCampaignStatus() == null) campaign.setCampaignStatus("READY");
        return campaigns.save(campaign);
    }
    private boolean birthdayOn(LocalDate dateOfBirth, LocalDate day) { return dateOfBirth != null && dateOfBirth.getMonthValue() == day.getMonthValue() && dateOfBirth.getDayOfMonth() == day.getDayOfMonth(); }
    private Map<String, Object> results(int targeted, int queued, int skipped, int optedOut, String status) { Map<String, Object> result = new HashMap<>(); result.put("status", status); result.put("targeted", targeted); result.put("queued", queued); result.put("skipped", skipped); result.put("optedOut", optedOut); return result; }
    private String validEmail(String value) { if (value == null) return null; String email=value.trim().toLowerCase(Locale.ROOT); return email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ? email : null; }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
