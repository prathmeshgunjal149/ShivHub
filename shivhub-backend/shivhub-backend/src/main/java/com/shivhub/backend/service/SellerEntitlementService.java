package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.config.RazorpayConfig;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.*;
import com.shivhub.backend.repository.*;

/**
 * Central source of seller subscription truth. Controllers and the subscription
 * request filter query this service instead of scattering plan-name checks.
 */
@Service
@Transactional
public class SellerEntitlementService {
    public static final String ONLINE_EXISTING_MOBILE_SALES = "ONLINE_EXISTING_MOBILE_SALES";
    public static final String ONLINE_ORDER_FULFILMENT_LIMITED = "ONLINE_ORDER_FULFILMENT_LIMITED";
    public static final String OFFLINE_POS = "OFFLINE_POS";
    public static final String PRODUCT_MANAGEMENT = "PRODUCT_MANAGEMENT";
    public static final String PURCHASE_MANAGEMENT = "PURCHASE_MANAGEMENT";
    public static final String INVENTORY_MANAGEMENT = "INVENTORY_MANAGEMENT";
    public static final String DISTRIBUTOR_MANAGEMENT = "DISTRIBUTOR_MANAGEMENT";
    public static final String CUSTOMER_MANAGEMENT = "CUSTOMER_MANAGEMENT";
    public static final String RECEIVABLE_MANAGEMENT = "RECEIVABLE_MANAGEMENT";
    public static final String EXPENSE_MANAGEMENT = "EXPENSE_MANAGEMENT";
    public static final String STAFF_MANAGEMENT = "STAFF_MANAGEMENT";
    public static final String GST_REPORTS = "GST_REPORTS";
    public static final String CA_REPORTS = "CA_REPORTS";
    public static final String ADVANCED_REPORTS = "ADVANCED_REPORTS";
    public static final String ANALYTICS = "ANALYTICS";
    public static final String MARKETING = "MARKETING";
    public static final String REFERRAL_MANAGEMENT = "REFERRAL_MANAGEMENT";

    private static final Set<String> SYSTEM_REQUIRED = Set.of("SELLER_LOGIN", "SELLER_DASHBOARD", "SELLER_SETTINGS", "SUBSCRIPTION_MANAGEMENT", "HISTORY_VIEW", "LOGOUT");
    private static final List<FeatureSeed> DEFAULT_FEATURES = List.of(
            new FeatureSeed(ONLINE_EXISTING_MOBILE_SALES, "Online existing mobile sales", "ONLINE SALES"),
            new FeatureSeed(ONLINE_ORDER_FULFILMENT_LIMITED, "Online order fulfilment", "ONLINE SALES"),
            new FeatureSeed(OFFLINE_POS, "Offline billing / POS", "SALES"),
            new FeatureSeed(PRODUCT_MANAGEMENT, "Product management", "INVENTORY"),
            new FeatureSeed(PURCHASE_MANAGEMENT, "Purchase management", "INVENTORY"),
            new FeatureSeed(INVENTORY_MANAGEMENT, "Inventory management", "INVENTORY"),
            new FeatureSeed(DISTRIBUTOR_MANAGEMENT, "Distributor management", "BUSINESS"),
            new FeatureSeed(CUSTOMER_MANAGEMENT, "Customer management", "BUSINESS"),
            new FeatureSeed(RECEIVABLE_MANAGEMENT, "Receivables", "FINANCE"),
            new FeatureSeed(EXPENSE_MANAGEMENT, "Expense management", "FINANCE"),
            new FeatureSeed(STAFF_MANAGEMENT, "Staff management", "BUSINESS"),
            new FeatureSeed(GST_REPORTS, "GST reports", "REPORTS"),
            new FeatureSeed(CA_REPORTS, "CA reports", "REPORTS"),
            new FeatureSeed(ADVANCED_REPORTS, "Advanced reports", "REPORTS"),
            new FeatureSeed(ANALYTICS, "Analytics", "REPORTS"),
            new FeatureSeed(MARKETING, "Marketing", "BUSINESS"),
            new FeatureSeed(REFERRAL_MANAGEMENT, "Referral management", "BUSINESS"));

    private final UserRepository users;
    private final SubscriptionPlanRepository plans;
    private final SubscriptionFeatureRepository features;
    private final PlanFeatureRepository planFeatures;
    private final SellerSubscriptionRepository subscriptions;
    private final SellerSubscriptionEntitlementRepository entitlements;
    private final SubscriptionPaymentRepository payments;
    private final SubscriptionAuditLogRepository audits;
    private final RazorpayConfig razorpay;
    private final EmailService emailService;

    public SellerEntitlementService(UserRepository users, SubscriptionPlanRepository plans, SubscriptionFeatureRepository features,
            PlanFeatureRepository planFeatures, SellerSubscriptionRepository subscriptions,
            SellerSubscriptionEntitlementRepository entitlements, SubscriptionPaymentRepository payments,
            SubscriptionAuditLogRepository audits, RazorpayConfig razorpay, EmailService emailService) {
        this.users = users; this.plans = plans; this.features = features; this.planFeatures = planFeatures;
        this.subscriptions = subscriptions; this.entitlements = entitlements; this.payments = payments;
        this.audits = audits; this.razorpay = razorpay; this.emailService = emailService;
    }

    /** Invoked only from the first successful admin approval. It never restarts a trial. */
    public SellerSubscription ensureFirstApprovalTrial(User seller) {
        if (seller == null || seller.getRole() != Role.SELLER) throw new IllegalArgumentException("Seller access required");
        return subscriptions.findBySellerId(seller.getId()).orElseGet(() -> {
            LocalDate today = LocalDate.now();
            SellerSubscription subscription = new SellerSubscription();
            subscription.setSeller(seller); subscription.setStatus(SubscriptionStatus.TRIAL);
            // Display the recommended, admin-controlled post-trial plan without
            // granting paid entitlements until an actual verified payment.
            subscription.setCurrentPlan(plans.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().findFirst().orElse(null));
            subscription.setTrialStartDate(today); subscription.setTrialEndDate(today.plusMonths(6));
            SellerSubscription saved = subscriptions.save(subscription);
            audit(saved, null, "TRIAL_STARTED", null, today + " to " + saved.getTrialEndDate(), "First admin approval");
            notifySeller(saved, "ShivHub free trial started", "Your seller account is approved. Your full-access six month trial ends on " + saved.getTrialEndDate() + ".");
            return saved;
        });
    }

    public SellerSubscriptionResponse sellerSubscription(String sellerEmail) {
        return responseFor(seller(sellerEmail));
    }

    public SubscriptionEntitlementsResponse entitlementResponse(String sellerEmail) {
        User seller = seller(sellerEmail);
        SellerSubscription subscription = subscriptions.findBySellerId(seller.getId()).orElse(null);
        refresh(subscription);
        boolean legacy = subscription == null;
        return new SubscriptionEntitlementsResponse(legacy ? "LEGACY_ACCESS" : subscription.getStatus().name(),
                subscription != null && subscription.getStatus() == SubscriptionStatus.TRIAL,
                subscription != null && requiresRenewal(subscription.getStatus()), legacy, effectiveFeatures(seller, subscription));
    }

    /** Unknown codes deliberately return false, even during trial. */
    public boolean hasFeature(String sellerEmail, String featureCode) { return hasFeature(seller(sellerEmail), featureCode); }

    public boolean hasFeature(User seller, String featureCode) {
        if (seller == null || seller.getRole() != Role.SELLER || featureCode == null) return false;
        String code = featureCode.trim().toUpperCase(Locale.ROOT);
        if (SYSTEM_REQUIRED.contains(code)) return true;
        SellerSubscription subscription = subscriptions.findBySellerId(seller.getId()).orElse(null);
        refresh(subscription);
        // Safe legacy compatibility: admins can view/backfill existing sellers without breaking their live tools.
        if (subscription == null) return true;
        if (subscription.getStatus() == SubscriptionStatus.TRIAL) return true;
        // POS is the seller's core paid workflow. Older active subscriptions may
        // have been created before OFFLINE_POS was seeded or before their
        // entitlement snapshot was backfilled; an active subscription must not
        // be reported as "no subscription" in Billing/Finance in that case.
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE && OFFLINE_POS.equals(code)) return true;
        if (features.findByFeatureCodeIgnoreCase(code).filter(SubscriptionFeature::isActive).isEmpty()) return false;
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            boolean snapshotEnabled = entitlements.findBySubscriptionId(subscription.getId()).stream()
                    .anyMatch(value -> value.isEnabled() && code.equalsIgnoreCase(value.getFeatureCode()));
            if (snapshotEnabled) return true;
            // Existing paid subscriptions can pre-date a newly seeded feature or
            // have an incomplete snapshot after an earlier deployment. The plan
            // is the authoritative fallback; this preserves admin feature
            // choices and fixes false "no active subscription" POS/Finance locks.
            return subscription.getCurrentPlan() != null && planFeatures.findByPlanId(subscription.getCurrentPlan().getId()).stream()
                    .anyMatch(item -> item.isEnabled() && code.equalsIgnoreCase(item.getFeature().getFeatureCode()));
        }
        // Explicit expired-account exception: online existing-mobile sales and required fulfilment remain available.
        return ONLINE_EXISTING_MOBILE_SALES.equals(code) || ONLINE_ORDER_FULFILMENT_LIMITED.equals(code);
    }

    public void requireFeature(String sellerEmail, String featureCode) {
        User seller = seller(sellerEmail);
        if (hasFeature(seller, featureCode)) return;
        SellerSubscription subscription = subscriptions.findBySellerId(seller.getId()).orElse(null);
        SubscriptionStatus status = subscription == null ? SubscriptionStatus.EXPIRED : subscription.getStatus();
        throw new SubscriptionRequiredException(status);
    }

    public List<SubscriptionPlanResponse> availablePlans() { return plans.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(this::planResponse).toList(); }
    public List<SubscriptionPlanResponse> allPlans() { return plans.findAllByOrderByDisplayOrderAscNameAsc().stream().map(this::planResponse).toList(); }
    public List<SubscriptionFeatureResponse> allFeatures() {
        return features.findAll().stream().sorted(Comparator.comparing(SubscriptionFeature::getCategory, Comparator.nullsLast(String::compareTo)).thenComparing(SubscriptionFeature::getFeatureName)).map(this::featureResponse).toList();
    }

    public SubscriptionPlanResponse savePlan(Long id, SubscriptionPlanRequest request) {
        if (request.price() == null || request.price().signum() < 0) throw new IllegalArgumentException("Plan price cannot be negative");
        String code = normalizedCode(request.code());
        SubscriptionPlan plan = id == null ? new SubscriptionPlan() : plans.findById(id).orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        plans.findByCodeIgnoreCase(code).filter(other -> !Objects.equals(other.getId(), plan.getId())).ifPresent(other -> { throw new IllegalArgumentException("Plan code already exists"); });
        plan.setName(request.name().trim()); plan.setCode(code); plan.setDescription(blankToNull(request.description()));
        plan.setPrice(money(request.price())); plan.setCurrency(blankToDefault(request.currency(), "INR").toUpperCase(Locale.ROOT));
        plan.setBillingInterval(request.billingInterval()); plan.setBillingIntervalCount(Math.max(1, request.billingIntervalCount() == null ? 1 : request.billingIntervalCount()));
        plan.setTrialMonths(Math.max(0, request.trialMonths() == null ? 0 : request.trialMonths()));
        plan.setActive(request.active()); plan.setRecommended(request.recommended()); plan.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        SubscriptionPlan saved = plans.save(plan);
        planFeatures.deleteByPlanId(saved.getId());
        Set<String> selected = request.featureCodes() == null ? Set.of() : request.featureCodes().stream().filter(Objects::nonNull).map(value -> value.trim().toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        for (String featureCode : selected) {
            SubscriptionFeature feature = features.findByFeatureCodeIgnoreCase(featureCode).orElseThrow(() -> new IllegalArgumentException("Unknown subscription feature: " + featureCode));
            if (feature.getAccessType() != SubscriptionAccessType.SUBSCRIPTION) throw new IllegalArgumentException("System-required features are not assigned through plans");
            PlanFeature assignment = new PlanFeature(); assignment.setPlan(saved); assignment.setFeature(feature); assignment.setEnabled(true); planFeatures.save(assignment);
        }
        return planResponse(saved);
    }

    public SubscriptionCheckoutResponse createCheckout(String sellerEmail, Long planId) {
        User seller = seller(sellerEmail);
        SubscriptionPlan plan = plans.findById(planId).filter(SubscriptionPlan::isActive).orElseThrow(() -> new IllegalArgumentException("Selected subscription plan is unavailable"));
        if (plan.getPrice().signum() <= 0) throw new IllegalArgumentException("This plan cannot be purchased through checkout");
        SellerSubscription subscription = subscriptions.findBySellerId(seller.getId()).orElseGet(() -> ensureFirstApprovalTrial(seller));
        SubscriptionPayment pending = payments.findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream().filter(item -> item.getPlan().getId().equals(planId))
                .filter(item -> item.getPaymentStatus() == SubscriptionPaymentStatus.CREATED || item.getPaymentStatus() == SubscriptionPaymentStatus.PENDING)
                .filter(item -> item.getGatewayOrderId() != null).findFirst().orElse(null);
        if (pending != null) return checkoutResponse(pending, seller, plan);
        if (!razorpay.isConfigured()) throw new IllegalStateException("Razorpay is not configured for subscription payments");
        SubscriptionPayment payment = new SubscriptionPayment(); payment.setSeller(seller); payment.setSellerSubscription(subscription); payment.setPlan(plan);
        payment.setAmount(money(plan.getPrice())); payment.setCurrency(plan.getCurrency()); payment.setPaymentStatus(SubscriptionPaymentStatus.CREATED); payment.setPaymentMethod("RAZORPAY");
        payment = payments.save(payment);
        try {
            JSONObject request = new JSONObject(); request.put("amount", paise(payment.getAmount())); request.put("currency", payment.getCurrency()); request.put("receipt", "SHSUB-" + payment.getId());
            JSONObject notes = new JSONObject(); notes.put("subscription_payment_id", payment.getId()); notes.put("seller_id", seller.getId()); request.put("notes", notes);
            com.razorpay.Order order = razorpay.client().orders.create(request);
            payment.setGatewayOrderId(order.get("id")); payments.save(payment);
            audit(subscription, seller.getId(), "PLAN_SELECTED", subscription.getCurrentPlan() == null ? null : subscription.getCurrentPlan().getCode(), plan.getCode(), "Secure payment initiated");
            return checkoutResponse(payment, seller, plan);
        } catch (Exception exception) { payment.setPaymentStatus(SubscriptionPaymentStatus.FAILED); payment.setFailureReason("Gateway order could not be created"); payments.save(payment); throw new IllegalStateException("Unable to create secure subscription payment"); }
    }

    public SellerSubscriptionResponse verifyCheckout(String sellerEmail, SubscriptionPaymentVerificationRequest request) {
        User seller = seller(sellerEmail);
        SubscriptionPayment payment = payments.findById(request.subscriptionPaymentId()).orElseThrow(() -> new IllegalArgumentException("Subscription payment not found"));
        if (!payment.getSeller().getId().equals(seller.getId()) || !Objects.equals(payment.getGatewayOrderId(), request.razorpayOrderId())) throw new SecurityException("Payment does not belong to this seller subscription");
        if (payment.getPaymentStatus() == SubscriptionPaymentStatus.SUCCESS) return responseFor(seller);
        verifySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        payments.findByGatewayPaymentId(request.razorpayPaymentId()).filter(existing -> !existing.getId().equals(payment.getId())).ifPresent(existing -> { throw new IllegalStateException("Gateway payment has already been used"); });
        payment.setGatewayPaymentId(request.razorpayPaymentId()); payment.setGatewaySignature(request.razorpaySignature()); payment.setPaymentStatus(SubscriptionPaymentStatus.SUCCESS); payment.setPaidAt(LocalDateTime.now());
        activatePayment(payment); payments.save(payment);
        notifySeller(payment.getSellerSubscription(), "ShivHub subscription payment successful", "Your " + payment.getPlan().getName() + " subscription is active until " + payment.getBillingPeriodEnd() + ".");
        return responseFor(seller);
    }

    /** Razorpay webhook counterpart to browser verification. Safe when Razorpay retries the same event. */
    public boolean completeGatewayPayment(String gatewayOrderId, String gatewayPaymentId) {
        if (gatewayOrderId == null || gatewayOrderId.isBlank()) return false;
        SubscriptionPayment payment = payments.findByGatewayOrderId(gatewayOrderId).orElse(null);
        if (payment == null) return false;
        if (payment.getPaymentStatus() == SubscriptionPaymentStatus.SUCCESS) return true;
        if (gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            payments.findByGatewayPaymentId(gatewayPaymentId).filter(existing -> !existing.getId().equals(payment.getId())).ifPresent(existing -> { throw new IllegalStateException("Gateway payment has already been used"); });
            payment.setGatewayPaymentId(gatewayPaymentId);
        }
        payment.setPaymentStatus(SubscriptionPaymentStatus.SUCCESS); payment.setPaidAt(LocalDateTime.now());
        activatePayment(payment); payments.save(payment);
        notifySeller(payment.getSellerSubscription(), "ShivHub subscription payment successful", "Your " + payment.getPlan().getName() + " subscription is active until " + payment.getBillingPeriodEnd() + ".");
        return true;
    }

    public List<SubscriptionPaymentResponse> sellerPayments(String sellerEmail) { return payments.findBySellerIdOrderByCreatedAtDesc(seller(sellerEmail).getId()).stream().map(this::paymentResponse).toList(); }
    public List<SubscriptionPaymentResponse> adminPayments() { return payments.findAllByOrderByCreatedAtDesc().stream().map(this::paymentResponse).toList(); }

    public Map<String, Object> adminOverview() {
        List<SellerSubscription> rows = subscriptions.findAllDetailed();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSellers", users.findByRole(Role.SELLER).size());
        for (SubscriptionStatus status : SubscriptionStatus.values()) result.put(status.name().toLowerCase(Locale.ROOT), rows.stream().filter(item -> item.getStatus() == status).count());
        LocalDate today = LocalDate.now();
        result.put("trialsEndingSoon", rows.stream().filter(item -> item.getStatus() == SubscriptionStatus.TRIAL && item.getTrialEndDate() != null && !item.getTrialEndDate().isAfter(today.plusDays(30))).count());
        result.put("renewalsDue", rows.stream().filter(item -> item.getNextBillingDate() != null && !item.getNextBillingDate().isAfter(today.plusDays(30))).count());
        List<SubscriptionPayment> successful = payments.findAllByOrderByCreatedAtDesc().stream().filter(payment -> payment.getPaymentStatus() == SubscriptionPaymentStatus.SUCCESS).toList();
        result.put("totalRevenue", successful.stream().map(SubscriptionPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("todayRevenue", successful.stream().filter(payment -> payment.getPaidAt() != null && payment.getPaidAt().toLocalDate().equals(today)).map(SubscriptionPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("monthlyRevenue", successful.stream().filter(payment -> payment.getPaidAt() != null && payment.getPaidAt().getYear() == today.getYear() && payment.getPaidAt().getMonth() == today.getMonth()).map(SubscriptionPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("successfulPayments", successful.size()); result.put("failedPayments", payments.countByPaymentStatus(SubscriptionPaymentStatus.FAILED));
        return result;
    }

    public List<SellerSubscriptionResponse> adminSubscriptions() { return users.findByRole(Role.SELLER).stream().map(this::responseFor).toList(); }

    public SellerSubscriptionResponse adminExtend(Long sellerId, SubscriptionAdminActionRequest request, Long adminId) {
        if (request.extensionDays() == null || request.extensionDays() < 1 || request.extensionDays() > 3650) throw new IllegalArgumentException("Enter a valid extension period");
        User seller = sellerById(sellerId); SellerSubscription subscription = subscriptions.findBySellerId(sellerId).orElseGet(() -> ensureFirstApprovalTrial(seller));
        LocalDate start = subscription.getSubscriptionEndDate() != null && subscription.getSubscriptionEndDate().isAfter(LocalDate.now()) ? subscription.getSubscriptionEndDate().plusDays(1) : LocalDate.now();
        if (subscription.getCurrentPlan() == null) subscription.setCurrentPlan(plans.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().findFirst().orElse(null));
        subscription.setSubscriptionStartDate(start); subscription.setSubscriptionEndDate(start.plusDays(request.extensionDays())); subscription.setNextBillingDate(subscription.getSubscriptionEndDate()); subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptions.save(subscription); snapshot(subscription, subscription.getCurrentPlan()); audit(subscription, adminId, "ADMIN_EXTENDED", null, String.valueOf(request.extensionDays()), request.reason());
        return responseFor(seller);
    }

    public SellerSubscriptionResponse adminSetSuspended(Long sellerId, boolean suspended, SubscriptionAdminActionRequest request, Long adminId) {
        User seller = sellerById(sellerId); SellerSubscription subscription = subscriptions.findBySellerId(sellerId).orElseGet(() -> ensureFirstApprovalTrial(seller));
        SubscriptionStatus before = subscription.getStatus(); subscription.setStatus(suspended ? SubscriptionStatus.SUSPENDED : (subscription.getSubscriptionEndDate() != null && !subscription.getSubscriptionEndDate().isBefore(LocalDate.now()) ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED));
        subscriptions.save(subscription); audit(subscription, adminId, suspended ? "ADMIN_SUSPENDED" : "ADMIN_REACTIVATED", before.name(), subscription.getStatus().name(), request.reason());
        return responseFor(seller);
    }

    public void processDailyLifecycle() {
        subscriptions.findAllDetailed().forEach(subscription -> { refresh(subscription); sendDueReminder(subscription); });
    }

    public void ensureDefaultConfiguration() {
        for (FeatureSeed seed : DEFAULT_FEATURES) features.findByFeatureCodeIgnoreCase(seed.code()).orElseGet(() -> {
            SubscriptionFeature feature = new SubscriptionFeature(); feature.setFeatureCode(seed.code()); feature.setFeatureName(seed.name()); feature.setCategory(seed.category()); feature.setAccessType(SubscriptionAccessType.SUBSCRIPTION); return features.save(feature);
        });
        if (plans.count() == 0) {
            SubscriptionPlan plan = new SubscriptionPlan(); plan.setName("ShivHub Seller Pro"); plan.setCode("SELLER_PRO"); plan.setDescription("Complete seller business tools."); plan.setPrice(new BigDecimal("399.00")); plan.setBillingInterval(SubscriptionBillingInterval.MONTH); plan.setBillingIntervalCount(1); plan.setRecommended(true); plan.setDisplayOrder(1); plan = plans.save(plan);
            for (SubscriptionFeature feature : features.findByActiveTrueOrderByCategoryAscFeatureNameAsc()) { PlanFeature item = new PlanFeature(); item.setPlan(plan); item.setFeature(feature); item.setEnabled(true); planFeatures.save(item); }
        }
    }

    private void activatePayment(SubscriptionPayment payment) {
        SellerSubscription subscription = payment.getSellerSubscription(); SubscriptionPlan plan = payment.getPlan();
        LocalDate today = LocalDate.now(); LocalDate start = subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getSubscriptionEndDate() != null && subscription.getSubscriptionEndDate().isAfter(today) ? subscription.getSubscriptionEndDate().plusDays(1) : today;
        LocalDate end = addInterval(start, plan).minusDays(1);
        subscription.setCurrentPlan(plan); subscription.setSubscriptionStartDate(start); subscription.setSubscriptionEndDate(end); subscription.setNextBillingDate(end.plusDays(1)); subscription.setStatus(SubscriptionStatus.ACTIVE); subscription.setPaymentGateway("RAZORPAY"); subscriptions.save(subscription);
        payment.setBillingPeriodStart(start); payment.setBillingPeriodEnd(end); snapshot(subscription, plan); audit(subscription, payment.getSeller().getId(), "PAYMENT_SUCCESS", null, plan.getCode(), "Verified Razorpay payment");
    }

    private void snapshot(SellerSubscription subscription, SubscriptionPlan plan) {
        entitlements.deleteBySubscriptionId(subscription.getId());
        if (plan == null) return;
        for (PlanFeature item : planFeatures.findByPlanId(plan.getId())) { SellerSubscriptionEntitlement snapshot = new SellerSubscriptionEntitlement(); snapshot.setSubscription(subscription); snapshot.setFeatureCode(item.getFeature().getFeatureCode()); snapshot.setEnabled(item.isEnabled()); entitlements.save(snapshot); }
    }

    private void refresh(SellerSubscription subscription) {
        if (subscription == null || subscription.getStatus() == SubscriptionStatus.SUSPENDED || subscription.getStatus() == SubscriptionStatus.CANCELLED) return;
        LocalDate today = LocalDate.now(); SubscriptionStatus before = subscription.getStatus();
        if (subscription.getStatus() == SubscriptionStatus.TRIAL && subscription.getTrialEndDate() != null && today.isAfter(subscription.getTrialEndDate())) subscription.setStatus(SubscriptionStatus.EXPIRED);
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getSubscriptionEndDate() != null && today.isAfter(subscription.getSubscriptionEndDate())) subscription.setStatus(SubscriptionStatus.EXPIRED);
        if (before != subscription.getStatus()) {
            subscriptions.save(subscription); audit(subscription, null, before == SubscriptionStatus.TRIAL ? "TRIAL_ENDED" : "EXPIRED", before.name(), subscription.getStatus().name(), "Automatic daily lifecycle check");
            if (subscription.getStatus() == SubscriptionStatus.EXPIRED) notifySeller(subscription, "Your ShivHub subscription has expired", "Your business data remains safe. Existing eligible mobile stock can still receive online orders, but Offline POS and premium tools are locked until renewal.");
        }
    }

    private Map<String, Boolean> effectiveFeatures(User seller, SellerSubscription subscription) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (SubscriptionFeature feature : features.findByActiveTrueOrderByCategoryAscFeatureNameAsc()) result.put(feature.getFeatureCode(), hasFeature(seller, feature.getFeatureCode()));
        return result;
    }

    private SellerSubscriptionResponse responseFor(User seller) {
        SellerSubscription subscription = subscriptions.findBySellerId(seller.getId()).orElse(null); refresh(subscription);
        boolean legacy = subscription == null;
        SubscriptionPlanResponse plan = subscription == null || subscription.getCurrentPlan() == null ? null : planResponse(subscription.getCurrentPlan());
        LocalDate finish = subscription == null ? null : subscription.getStatus() == SubscriptionStatus.TRIAL ? subscription.getTrialEndDate() : subscription.getSubscriptionEndDate();
        long remaining = finish == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), finish));
        return new SellerSubscriptionResponse(subscription == null ? null : subscription.getId(), seller.getId(), seller.getName(), seller.getBusinessName(), plan,
                legacy ? "LEGACY_ACCESS" : subscription.getStatus().name(), subscription == null ? null : subscription.getTrialStartDate(), subscription == null ? null : subscription.getTrialEndDate(),
                subscription == null ? null : subscription.getSubscriptionStartDate(), subscription == null ? null : subscription.getSubscriptionEndDate(), subscription == null ? null : subscription.getNextBillingDate(),
                subscription != null && subscription.isAutoRenewEnabled(), remaining, legacy, effectiveFeatures(seller, subscription));
    }

    private SubscriptionPlanResponse planResponse(SubscriptionPlan plan) { return new SubscriptionPlanResponse(plan.getId(), plan.getName(), plan.getCode(), plan.getDescription(), plan.getPrice(), plan.getCurrency(), plan.getBillingInterval(), plan.getBillingIntervalCount(), plan.getTrialMonths(), plan.isActive(), plan.isRecommended(), plan.getDisplayOrder(), planFeatures.findByPlanId(plan.getId()).stream().filter(PlanFeature::isEnabled).map(item -> item.getFeature().getFeatureCode()).toList()); }
    private SubscriptionFeatureResponse featureResponse(SubscriptionFeature feature) { return new SubscriptionFeatureResponse(feature.getId(), feature.getFeatureCode(), feature.getFeatureName(), feature.getDescription(), feature.getCategory(), feature.getAccessType(), feature.isActive()); }
    private SubscriptionPaymentResponse paymentResponse(SubscriptionPayment payment) { return new SubscriptionPaymentResponse(payment.getId(), payment.getPlan().getName(), payment.getAmount(), payment.getCurrency(), payment.getPaymentStatus().name(), payment.getPaymentMethod(), payment.getGatewayOrderId(), payment.getGatewayPaymentId(), payment.getBillingPeriodStart(), payment.getBillingPeriodEnd(), payment.getPaidAt(), payment.getFailureReason(), payment.getCreatedAt()); }
    private SubscriptionCheckoutResponse checkoutResponse(SubscriptionPayment payment, User seller, SubscriptionPlan plan) { return new SubscriptionCheckoutResponse(payment.getId(), payment.getGatewayOrderId(), paise(payment.getAmount()), payment.getCurrency(), razorpay.getKeyId(), seller.getName(), seller.getEmail(), seller.getMobile(), "ShivHub " + plan.getName() + " subscription"); }
    private User seller(String email) { return users.findByEmail(email).filter(user -> user.getRole() == Role.SELLER).orElseThrow(() -> new SecurityException("Seller access required")); }
    private User sellerById(Long id) { return users.findById(id).filter(user -> user.getRole() == Role.SELLER).orElseThrow(() -> new IllegalArgumentException("Seller not found")); }
    private boolean requiresRenewal(SubscriptionStatus status) { return status == SubscriptionStatus.EXPIRED || status == SubscriptionStatus.PAST_DUE || status == SubscriptionStatus.PAYMENT_PENDING || status == SubscriptionStatus.CANCELLED; }
    private LocalDate addInterval(LocalDate start, SubscriptionPlan plan) { int amount = Math.max(1, plan.getBillingIntervalCount()); return switch (plan.getBillingInterval()) { case MONTH -> start.plusMonths(amount); case QUARTER -> start.plusMonths(3L * amount); case HALF_YEAR -> start.plusMonths(6L * amount); case YEAR -> start.plusYears(amount); }; }
    private void audit(SellerSubscription subscription, Long actorId, String event, String oldValue, String newValue, String reason) { SubscriptionAuditLog item = new SubscriptionAuditLog(); item.setSubscription(subscription); item.setActorUserId(actorId); item.setEventType(event); item.setOldValue(oldValue); item.setNewValue(newValue); item.setReason(reason); audits.save(item); }
    private void sendDueReminder(SellerSubscription subscription) {
        if (subscription == null || (subscription.getStatus() != SubscriptionStatus.TRIAL && subscription.getStatus() != SubscriptionStatus.ACTIVE)) return;
        LocalDate end = subscription.getStatus() == SubscriptionStatus.TRIAL ? subscription.getTrialEndDate() : subscription.getSubscriptionEndDate();
        if (end == null) return;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), end);
        if (!Set.of(30L, 15L, 7L, 3L, 1L).contains(days)) return;
        String event = "RENEWAL_REMINDER_" + days;
        if (audits.findBySubscriptionIdOrderByCreatedAtDesc(subscription.getId()).stream().anyMatch(item -> event.equals(item.getEventType()))) return;
        audit(subscription, null, event, null, String.valueOf(days), "Scheduled reminder");
        notifySeller(subscription, "Your ShivHub subscription is ending soon", "Your " + (subscription.getStatus() == SubscriptionStatus.TRIAL ? "free trial" : "subscription") + " ends in " + days + " days. Renew to keep subscribed business tools available.");
    }
    private void notifySeller(SellerSubscription subscription, String subject, String message) { try { User seller = subscription.getSeller(); emailService.sendSubscriptionNotice(seller.getEmail(), seller.getName(), subject, message); } catch (Exception ignored) { /* Notifications never roll back subscription state. */ } }
    private void verifySignature(String orderId, String paymentId, String signature) { if (!razorpay.isConfigured() || !constantTimeEquals(hmac(orderId + "|" + paymentId, razorpay.getKeySecret()), signature)) throw new SecurityException("Invalid Razorpay payment signature"); }
    private String hmac(String payload, String secret) { try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException("Unable to verify payment signature"); } }
    private boolean constantTimeEquals(String left, String right) { return left != null && right != null && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private long paise(BigDecimal value) { return money(value).movePointRight(2).longValueExact(); }
    private String normalizedCode(String code) { String result = code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_\\-]", "_"); if (result.isBlank()) throw new IllegalArgumentException("Plan code is required"); return result; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String blankToDefault(String value, String fallback) { String result = blankToNull(value); return result == null ? fallback : result; }
    private record FeatureSeed(String code, String name, String category) { }
}
