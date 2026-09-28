package com.shivhub.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.MarketingCampaignRequest;
import com.shivhub.backend.entity.MarketingCampaign;
import com.shivhub.backend.entity.MarketingDelivery;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.MarketingCampaignRepository;
import com.shivhub.backend.repository.MarketingDeliveryRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class MarketingService {
    private final MarketingCampaignRepository campaigns;
    private final UserRepository users;
    private final ProductRepository products;
    private final EmailService emailService;
    private final MarketingDeliveryRepository deliveries;
    public MarketingService(MarketingCampaignRepository campaigns, UserRepository users, ProductRepository products, EmailService emailService, MarketingDeliveryRepository deliveries) {
        this.campaigns = campaigns; this.users = users; this.products = products; this.emailService = emailService; this.deliveries = deliveries;
    }
    public List<MarketingCampaign> list(String type) {
        return type == null || type.isBlank() ? campaigns.findAll() : campaigns.findByTypeOrderByCreatedAtDesc(normalize(type));
    }
    public MarketingCampaign get(Long id) { return campaigns.findById(id).orElseThrow(() -> new RuntimeException("Marketing item not found")); }
    @Transactional public MarketingCampaign create(MarketingCampaignRequest request) {
        MarketingCampaign item = new MarketingCampaign(); apply(item, request);
        MarketingCampaign saved = campaigns.save(item);
        if (request.isSendEmail() || request.getProductId() != null || request.getTargetCustomerId() != null) sendEmail(saved);
        return saved;
    }
    @Transactional public MarketingCampaign update(Long id, MarketingCampaignRequest request) {
        MarketingCampaign item = get(id); apply(item, request);
        MarketingCampaign saved = campaigns.save(item);
        if (request.isSendEmail() || request.getTargetCustomerId() != null) sendEmail(saved);
        return saved;
    }
    public void delete(Long id) { campaigns.delete(get(id)); }
    public List<MarketingCampaign> activeFor(String audience) {
        LocalDateTime now = LocalDateTime.now();
        return campaigns.findByActiveTrueAndAudienceInAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByCreatedAtDesc(
                List.of(normalize(audience), "ALL"), now, now);
    }
    public Map<String, Integer> sendEmail(Long id) { return sendEmail(get(id)); }
    public List<MarketingDelivery> history(Long id) { get(id); return deliveries.findByCampaignIdOrderByCreatedAtDesc(id); }
    private Map<String, Integer> sendEmail(MarketingCampaign item) {
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        if (item.getTargetCustomerId() != null) {
            User customer = users.findById(item.getTargetCustomerId())
                    .orElseThrow(() -> new RuntimeException("Selected customer not found"));
            if (!eligible(customer)) {
                record(item, customer, "SKIPPED", customer.isMarketingOptOut() ? "Customer opted out" : "Customer disabled or email missing");
                return Map.of("sent", 0, "skipped", 1, "failed", 0);
            }
            try {
                emailService.sendMarketingEmail(customer.getEmail(), customer.getName(), item);
                record(item, customer, "SENT", "Email sent");
                return Map.of("sent", 1, "skipped", 0, "failed", 0);
            } catch (Exception exception) {
                record(item, customer, "FAILED", exception.getMessage());
                return Map.of("sent", 0, "skipped", 0, "failed", 1);
            }
        }
        List<User> recipients = switch (item.getAudience()) {
            case "CUSTOMER" -> users.findByRole(Role.CUSTOMER);
            case "SELLER" -> users.findByRole(Role.SELLER);
            default -> List.of();
        };
        if ("ALL".equals(item.getAudience())) {
            recipients = java.util.stream.Stream.concat(users.findByRole(Role.CUSTOMER).stream(), users.findByRole(Role.SELLER).stream()).toList();
        }
        for (User user : recipients) {
            if (!eligible(user)) {
                record(item, user, "SKIPPED", user.isMarketingOptOut() ? "Customer opted out" : "Disabled or email missing");
                skipped++;
                continue;
            }
            try {
                emailService.sendMarketingEmail(user.getEmail(), user.getName(), item);
                record(item, user, "SENT", "Email sent");
                sent++;
            }
            catch (Exception exception) {
                record(item, user, "FAILED", exception.getMessage());
                failed++;
            }
        }
        return Map.of("sent", sent, "skipped", skipped, "failed", failed);
    }
    private void apply(MarketingCampaign item, MarketingCampaignRequest request) {
        item.setType(normalize(request.getType())); item.setAudience(normalize(request.getAudience()));
        item.setTitle(request.getTitle().trim()); item.setDescription(request.getDescription());
        item.setBannerUrl(emptyToNull(request.getBannerUrl()));
        item.setPlacement(emptyToNull(request.getPlacement()));
        item.setTargetFilters(emptyToNull(request.getTargetFilters()));
        item.setCouponCode(blankToNull(request.getCouponCode())); item.setDiscountPercent(request.getDiscountPercent());
        item.setProductId(request.getProductId());
        item.setTargetCustomerId(request.getTargetCustomerId());
        if (request.getTargetCustomerId() != null) {
            User customer = users.findById(request.getTargetCustomerId())
                    .orElseThrow(() -> new RuntimeException("Selected customer not found"));
            if (customer.getRole() != Role.CUSTOMER) throw new IllegalArgumentException("A targeted coupon can only be assigned to a customer");
            item.setAudience("CUSTOMER");
        }
        if (request.getProductId() != null) {
            if (request.getDiscountPercent() == null || request.getDiscountPercent().signum() <= 0 || request.getDiscountPercent().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("A product offer requires a discount between 0 and 100%");
            }
            var product = products.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Selected product not found"));
            product.setOfferPercentage(request.getDiscountPercent());
            products.save(product);
            item.setAudience("CUSTOMER");
        }
        item.setActive(request.isActive()); item.setStartsAt(request.getStartsAt() == null ? LocalDateTime.now() : request.getStartsAt());
        item.setEndsAt(request.getEndsAt() == null ? LocalDateTime.now().plusDays(30) : request.getEndsAt());
        if (item.getEndsAt().isBefore(item.getStartsAt())) throw new IllegalArgumentException("End date must be after start date");
    }
    private String normalize(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private boolean eligible(User user) {
        return user.isEnabled()
                && user.getEmail() != null
                && !user.getEmail().isBlank()
                && !user.isMarketingOptOut();
    }
    private void record(MarketingCampaign campaign, User user, String status, String message) {
        MarketingDelivery delivery = new MarketingDelivery();
        delivery.setCampaign(campaign);
        delivery.setRecipientUserId(user.getId());
        delivery.setRecipientEmail(user.getEmail());
        delivery.setRecipientName(user.getName());
        delivery.setStatus(status);
        delivery.setMessage(message == null ? "" : message);
        deliveries.save(delivery);
    }
}
