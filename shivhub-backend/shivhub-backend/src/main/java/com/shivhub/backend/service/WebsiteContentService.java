package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.EnquiryRequest;
import com.shivhub.backend.dto.EnquiryResponse;
import com.shivhub.backend.dto.FaqRequest;
import com.shivhub.backend.dto.FaqResponse;
import com.shivhub.backend.dto.PolicyPageResponse;
import com.shivhub.backend.dto.PolicyUpdateRequest;
import com.shivhub.backend.dto.PolicyVersionResponse;
import com.shivhub.backend.dto.SiteSettingResponse;
import com.shivhub.backend.dto.UpdateEnquiryStatusRequest;
import com.shivhub.backend.dto.UpdateSiteSettingRequest;
import com.shivhub.backend.entity.CustomerEnquiry;
import com.shivhub.backend.entity.PolicyPage;
import com.shivhub.backend.entity.PolicyVersion;
import com.shivhub.backend.entity.SiteFaq;
import com.shivhub.backend.entity.SiteSetting;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.CustomerEnquiryRepository;
import com.shivhub.backend.repository.PolicyPageRepository;
import com.shivhub.backend.repository.PolicyVersionRepository;
import com.shivhub.backend.repository.SiteFaqRepository;
import com.shivhub.backend.repository.SiteSettingRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class WebsiteContentService {

    public static final Set<String> ALLOWED_POLICY_SLUGS = Set.of(
            "about", "contact", "terms", "privacy-policy", "cancellation-refund",
            "shipping-policy", "return-replacement", "warranty-policy", "help",
            "grievance", "seller-terms", "payment-security", "cookie-policy",
            "sitemap", "careers", "why-choose-us", "seller-support",
            "partner-program", "api-integration"
    );

    private static final Set<String> ALLOWED_ENQUIRY_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED");

    private final SiteSettingRepository settingRepository;
    private final PolicyPageRepository policyRepository;
    private final PolicyVersionRepository versionRepository;
    private final SiteFaqRepository faqRepository;
    private final CustomerEnquiryRepository enquiryRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WebsiteContentService(
            SiteSettingRepository settingRepository,
            PolicyPageRepository policyRepository,
            PolicyVersionRepository versionRepository,
            SiteFaqRepository faqRepository,
            CustomerEnquiryRepository enquiryRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.settingRepository = settingRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.faqRepository = faqRepository;
        this.enquiryRepository = enquiryRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public SiteSettingResponse publicSettings() {
        return toSetting(settingRepository.findAll().stream().findFirst().orElseGet(this::defaultSetting));
    }

    @Transactional
    public SiteSettingResponse updateSettings(UpdateSiteSettingRequest request) {
        SiteSetting setting = settingRepository.findAll().stream().findFirst().orElseGet(this::defaultSetting);
        setting.setBrandName(cleanRequired(request.brandName()));
        setting.setLegalBusinessName(cleanRequired(request.legalBusinessName()));
        setting.setSupportEmail(cleanRequired(request.supportEmail()));
        setting.setSupportPhone(clean(request.supportPhone()));
        setting.setBusinessAddress(clean(request.businessAddress()));
        setting.setSupportHours(clean(request.supportHours()));
        setting.setGrievanceContactName(clean(request.grievanceContactName()));
        setting.setGrievanceEmail(clean(request.grievanceEmail()));
        setting.setWebsiteUrl(validUrlOrBlank(request.websiteUrl()));
        setting.setInstagramUrl(validUrlOrBlank(request.instagramUrl()));
        setting.setFacebookUrl(validUrlOrBlank(request.facebookUrl()));
        setting.setWhatsappUrl(validUrlOrBlank(request.whatsappUrl()));
        setting.setYoutubeUrl(validUrlOrBlank(request.youtubeUrl()));
        setting.setEnabledPaymentMethods(clean(request.enabledPaymentMethods()));
        setting.setDemoData(Boolean.TRUE.equals(request.demoData()));
        setting.setRequiresReview(Boolean.TRUE.equals(request.requiresReview()));
        return toSetting(settingRepository.save(setting));
    }

    @Transactional(readOnly = true)
    public List<PolicyPageResponse> listPolicies(boolean admin) {
        return policyRepository.findAll().stream()
                .sorted((first, second) -> first.getSlug().compareTo(second.getSlug()))
                .map(policy -> toPolicy(policy, admin))
                .toList();
    }

    @Transactional(readOnly = true)
    public PolicyPageResponse getPublicPolicy(String slug) {
        validateSlug(slug);
        PolicyPage page = policyRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Policy page not found"));
        if (!page.isPublished() || page.getPublishedContent() == null || page.getPublishedContent().isBlank()) {
            throw new RuntimeException("Policy page is not published");
        }
        return toPolicy(page, false);
    }

    @Transactional(readOnly = true)
    public PolicyPageResponse getAdminPolicy(String slug) {
        validateSlug(slug);
        return policyRepository.findBySlug(slug).map(policy -> toPolicy(policy, true))
                .orElseThrow(() -> new RuntimeException("Policy page not found"));
    }

    @Transactional
    public PolicyPageResponse saveDraft(String slug, PolicyUpdateRequest request, String adminEmail) {
        validateSlug(slug);
        User admin = admin(adminEmail);
        PolicyPage page = policyRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Policy page not found"));
        page.setTitle(cleanRequired(request.title()));
        page.setDescription(clean(request.description()));
        page.setDraftContent(sanitizeContent(request.content()));
        page.setEffectiveDate(request.effectiveDate());
        page.setLastEditedAt(LocalDateTime.now());
        page.setLastEditedByAdminId(admin.getId());
        applyReviewFlags(page, request);
        policyRepository.save(page);
        saveVersion(page, "DRAFT", admin.getId(), page.getDraftContent());
        return toPolicy(page, true);
    }

    @Transactional
    public PolicyPageResponse publish(String slug, PolicyUpdateRequest request, String adminEmail) {
        validateSlug(slug);
        User admin = admin(adminEmail);
        PolicyPage page = policyRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Policy page not found"));
        applyReviewFlags(page, request);
        if (!page.isRequiresReview() && page.isDemoData() && !Boolean.TRUE.equals(request.confirmProductionReady())) {
            throw new RuntimeException("Confirm review before marking demo policy as production-ready");
        }
        page.setTitle(cleanRequired(request.title()));
        page.setDescription(clean(request.description()));
        page.setPublishedContent(sanitizeContent(request.content()));
        page.setDraftContent(null);
        page.setEffectiveDate(request.effectiveDate());
        page.setPublished(true);
        page.setLastPublishedAt(LocalDateTime.now());
        page.setLastPublishedByAdminId(admin.getId());
        page.setLastEditedAt(LocalDateTime.now());
        page.setLastEditedByAdminId(admin.getId());
        policyRepository.save(page);
        saveVersion(page, "PUBLISHED", admin.getId(), page.getPublishedContent());
        return toPolicy(page, true);
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> versions(String slug) {
        validateSlug(slug);
        PolicyPage page = policyRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Policy page not found"));
        return versionRepository.findByPolicyPageIdOrderByCreatedAtDesc(page.getId())
                .stream()
                .map(version -> new PolicyVersionResponse(version.getId(), version.getTitle(), version.getContent(), version.getAction(), version.getAdminId(), version.getCreatedAt()))
                .toList();
    }

    @Transactional
    public PolicyPageResponse restoreVersion(String slug, Long versionId, String adminEmail) {
        validateSlug(slug);
        User admin = admin(adminEmail);
        PolicyPage page = policyRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Policy page not found"));
        PolicyVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Policy version not found"));
        if (!version.getPolicyPage().getId().equals(page.getId())) {
            throw new RuntimeException("Version does not belong to this policy");
        }
        page.setTitle(version.getTitle());
        page.setDraftContent(version.getContent());
        page.setLastEditedAt(LocalDateTime.now());
        page.setLastEditedByAdminId(admin.getId());
        policyRepository.save(page);
        saveVersion(page, "RESTORED_AS_DRAFT", admin.getId(), page.getDraftContent());
        return toPolicy(page, true);
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> publicFaqs() {
        return faqRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(this::toFaq).toList();
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> adminFaqs() {
        return faqRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().map(this::toFaq).toList();
    }

    @Transactional
    public FaqResponse saveFaq(FaqRequest request) {
        SiteFaq faq = new SiteFaq();
        faq.setCategory(cleanRequired(request.category()));
        faq.setQuestion(cleanRequired(request.question()));
        faq.setAnswer(sanitizeContent(request.answer()));
        faq.setActive(request.active() == null || request.active());
        faq.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        faq.setDemoData(Boolean.TRUE.equals(request.demoData()));
        return toFaq(faqRepository.save(faq));
    }

    @Transactional
    public void deleteFaq(Long id) {
        faqRepository.deleteById(id);
    }

    @Transactional
    public EnquiryResponse submitEnquiry(EnquiryRequest request) {
        String email = cleanRequired(request.email()).toLowerCase();
        long recentCount = enquiryRepository.countByEmailAndCreatedAtAfter(email, LocalDateTime.now().minusHours(1));
        if (recentCount >= 5) {
            throw new RuntimeException("Too many support requests. Please try again later.");
        }
        CustomerEnquiry enquiry = new CustomerEnquiry();
        enquiry.setTicketReference("SH-TKT-" + System.currentTimeMillis());
        enquiry.setName(cleanRequired(request.name()));
        enquiry.setEmail(email);
        enquiry.setMobile(clean(request.mobile()));
        enquiry.setCategory(cleanRequired(request.category()));
        enquiry.setSubject(cleanRequired(request.subject()));
        enquiry.setMessage(sanitizeContent(request.message()));
        enquiry.setOrderReference(clean(request.orderReference()));
        CustomerEnquiry saved = enquiryRepository.save(enquiry);
        eventPublisher.publishEvent(new CustomerEnquiryNotificationEvent(
                CustomerEnquiryNotificationEvent.Type.RECEIVED, saved.getTicketReference(), saved.getName(), saved.getEmail(),
                saved.getCategory(), saved.getSubject(), saved.getStatus(), null));
        return toEnquiry(saved, true);
    }

    @Transactional(readOnly = true)
    public List<EnquiryResponse> enquiries(String status) {
        List<CustomerEnquiry> rows = status == null || status.isBlank()
                ? enquiryRepository.findTop50ByOrderByCreatedAtDesc()
                : enquiryRepository.findTop50ByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        return rows.stream().map(row -> toEnquiry(row, false)).toList();
    }

    @Transactional
    public EnquiryResponse updateEnquiry(Long id, UpdateEnquiryStatusRequest request, String adminEmail) {
        User admin = admin(adminEmail);
        CustomerEnquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        String status = request.status().trim().toUpperCase();
        if (!ALLOWED_ENQUIRY_STATUSES.contains(status)) {
            throw new RuntimeException("Invalid enquiry status");
        }
        boolean statusChanged = !status.equals(enquiry.getStatus());
        String customerResponse = clean(request.customerResponse());
        boolean responseChanged = !Objects.equals(customerResponse, enquiry.getCustomerResponse());
        enquiry.setStatus(status);
        enquiry.setInternalNotes(clean(request.internalNotes()));
        enquiry.setCustomerResponse(customerResponse);
        enquiry.setLastStatusChangedAt(LocalDateTime.now());
        enquiry.setLastStatusChangedByAdminId(admin.getId());
        CustomerEnquiry saved = enquiryRepository.save(enquiry);
        if (statusChanged || (responseChanged && customerResponse != null && !customerResponse.isBlank())) {
            eventPublisher.publishEvent(new CustomerEnquiryNotificationEvent(
                    CustomerEnquiryNotificationEvent.Type.UPDATED, saved.getTicketReference(), saved.getName(), saved.getEmail(),
                    saved.getCategory(), saved.getSubject(), saved.getStatus(), saved.getCustomerResponse()));
        }
        return toEnquiry(saved, false);
    }

    private void saveVersion(PolicyPage page, String action, Long adminId, String content) {
        PolicyVersion version = new PolicyVersion();
        version.setPolicyPage(page);
        version.setTitle(page.getTitle());
        version.setContent(content == null ? "" : content);
        version.setAction(action);
        version.setAdminId(adminId);
        versionRepository.save(version);
    }

    private void applyReviewFlags(PolicyPage page, PolicyUpdateRequest request) {
        if (request.demoData() != null) page.setDemoData(request.demoData());
        if (request.requiresReview() != null) page.setRequiresReview(request.requiresReview());
    }

    private SiteSetting defaultSetting() {
        SiteSetting setting = new SiteSetting();
        setting.setBrandName("ShivHub");
        setting.setLegalBusinessName("ShivHub Demo Commerce");
        setting.setSupportEmail("support@shivhub.example");
        setting.setSupportPhone("DEMO-NON-DIALABLE");
        setting.setBusinessAddress("Demo business address — replace before launch.");
        setting.setSupportHours("Demo: Monday–Saturday, 10 AM–6 PM.");
        setting.setGrievanceContactName("Demo Support Officer");
        setting.setEnabledPaymentMethods("");
        return setting;
    }

    private void validateSlug(String slug) {
        if (slug == null || !ALLOWED_POLICY_SLUGS.contains(slug)) {
            throw new RuntimeException("Invalid policy page");
        }
    }

    private User admin(String email) {
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Admin access required");
        return admin;
    }

    private String sanitizeContent(String value) {
        return cleanRequired(value).replaceAll("<[^>]*>", "").trim();
    }

    private String cleanRequired(String value) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.isBlank()) throw new RuntimeException("Required field is missing");
        return cleaned;
    }

    private String clean(String value) {
        return value == null ? null : value.replace('\u0000', ' ').trim();
    }

    private String validUrlOrBlank(String value) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.isBlank()) return null;
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            throw new RuntimeException("Only http/https URLs are allowed");
        }
        return cleaned;
    }

    private SiteSettingResponse toSetting(SiteSetting setting) {
        return new SiteSettingResponse(setting.getId(), setting.getBrandName(), setting.getLegalBusinessName(),
                setting.getSupportEmail(), setting.getSupportPhone(), setting.getBusinessAddress(),
                setting.getSupportHours(), setting.getGrievanceContactName(), setting.getGrievanceEmail(),
                setting.getWebsiteUrl(), setting.getInstagramUrl(), setting.getFacebookUrl(), setting.getWhatsappUrl(),
                setting.getYoutubeUrl(), setting.getEnabledPaymentMethods(), setting.isDemoData(), setting.isRequiresReview());
    }

    private PolicyPageResponse toPolicy(PolicyPage page, boolean admin) {
        return new PolicyPageResponse(page.getId(), page.getSlug(), page.getTitle(), page.getDescription(),
                page.getPublishedContent(), admin ? page.getDraftContent() : null, page.getEffectiveDate(),
                page.isPublished(), page.isDemoData(), page.isRequiresReview(), page.getLastPublishedAt(),
                admin ? page.getLastPublishedByAdminId() : null, page.getLastEditedAt(),
                admin ? page.getLastEditedByAdminId() : null);
    }

    private FaqResponse toFaq(SiteFaq faq) {
        return new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer(), faq.isActive(),
                faq.getDisplayOrder(), faq.isDemoData());
    }

    private EnquiryResponse toEnquiry(CustomerEnquiry enquiry, boolean publicSafe) {
        return new EnquiryResponse(enquiry.getId(), enquiry.getTicketReference(), enquiry.getName(), enquiry.getEmail(),
                enquiry.getMobile(), enquiry.getCategory(), enquiry.getSubject(), enquiry.getMessage(),
                enquiry.getOrderReference(), enquiry.getStatus(), publicSafe ? null : enquiry.getInternalNotes(), publicSafe ? null : enquiry.getCustomerResponse(),
                enquiry.getCreatedAt(), enquiry.getUpdatedAt());
    }
}
