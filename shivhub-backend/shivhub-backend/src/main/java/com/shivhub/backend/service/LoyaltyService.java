package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.shivhub.backend.dto.LoyaltyAdjustmentRequest;
import com.shivhub.backend.dto.LoyaltySettingsRequest;
import com.shivhub.backend.dto.CustomerLoyaltySummaryResponse;
import com.shivhub.backend.dto.CustomerLoyaltyTransactionResponse;
import com.shivhub.backend.entity.CustomerLoyaltyWallet;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.LoyaltyPointTransaction;
import com.shivhub.backend.entity.LoyaltySettings;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.ServiceRequest;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.CustomerLoyaltyWalletRepository;
import com.shivhub.backend.repository.CustomerProfileRepository;
import com.shivhub.backend.repository.LoyaltyPointTransactionRepository;
import com.shivhub.backend.repository.LoyaltySettingsRepository;
import com.shivhub.backend.repository.UserRepository;

/** Transactional, idempotent points ledger shared by POS and online checkout. */
@Service
public class LoyaltyService {
    /** ShivHub's fixed customer reward conversion: three whole points make one rupee. */
    private static final long POINTS_PER_RUPEE = 3L;
    private final CustomerLoyaltyWalletRepository wallets;
    private final LoyaltyPointTransactionRepository transactions;
    private final LoyaltySettingsRepository settings;
    private final CustomerProfileRepository profiles;
    private final UserRepository users;
    private final EmailService emailService;

    public LoyaltyService(CustomerLoyaltyWalletRepository wallets, LoyaltyPointTransactionRepository transactions,
                          LoyaltySettingsRepository settings, CustomerProfileRepository profiles, UserRepository users,
                          EmailService emailService) {
        this.wallets = wallets; this.transactions = transactions; this.settings = settings;
        this.profiles = profiles; this.users = users; this.emailService = emailService;
    }

    @Transactional
    public void awardForOfflineBill(OfflineBill bill) {
        if (bill == null || bill.getId() == null || bill.getPaymentStatus() != PaymentStatus.PAID) return;
        CustomerProfile profile = profileForBill(bill);
        if (profile == null) return;

        // Redeem against the customer's pre-sale balance. New points from the same
        // bill must never make an otherwise-invalid redemption possible.
        long redeemedPoints = redeemForOfflineBill(bill, profile);
        long earnedPoints = 0;
        LoyaltySettings rule = ruleFor(bill.getSellerId());
        if (transactions.findBySourceTypeAndSourceIdAndTransactionType("OFFLINE_BILL", bill.getId(), "EARN").isEmpty()) {
            earnedPoints = earned(rule, bill.getGrandTotal());
            if (earnedPoints > 0) append(profile, "EARN", "OFFLINE_BILL", bill.getId(), bill.getSellerId(), earnedPoints,
                    "Points earned for POS bill " + bill.getBillNumber());
        }
        if (earnedPoints > 0 || redeemedPoints > 0) {
            CustomerLoyaltyWallet wallet = wallet(profile);
            scheduleLoyaltyEmail(profile, bill.getCustomerEmail(), bill.getCustomerName(),
                    "POS bill " + bill.getBillNumber(), earnedPoints, redeemedPoints,
                    wallet.getAvailablePoints(), bill.getLoyaltyDiscount());
        }
    }

    @Transactional
    public void awardForOnlineOrder(Order order) {
        if (order == null || order.getId() == null || order.getPaymentStatus() != PaymentStatus.PAID
                || order.getOrderStatus() == com.shivhub.backend.enums.OrderStatus.CANCELLED) return;
        CustomerProfile profile = profileForUser(order.getCustomerId());
        if (profile == null || transactions.findBySourceTypeAndSourceIdAndTransactionType("ONLINE_ORDER", order.getId(), "EARN").isPresent()) return;
        LoyaltySettings rule = ruleFor(null);
        long points = earned(rule, order.getGrandTotal());
        if (points > 0) {
            append(profile, "EARN", "ONLINE_ORDER", order.getId(), null, points,
                    "Points earned for online order " + order.getOrderNumber());
            CustomerLoyaltyWallet wallet = wallet(profile);
            scheduleLoyaltyEmail(profile, null, null, "online order " + order.getOrderNumber(),
                    points, 0, wallet.getAvailablePoints(), BigDecimal.ZERO);
        }
    }

    @Transactional
    public void reverseForOfflineBill(OfflineBill bill, String reason) {
        reverse(profileForBill(bill), "OFFLINE_BILL", bill == null ? null : bill.getId(), bill == null ? null : bill.getSellerId(), reason);
    }

    @Transactional
    public void reverseForOnlineOrder(Order order, String reason) {
        CustomerProfile profile = profileForUser(order == null ? null : order.getCustomerId());
        Long orderId = order == null ? null : order.getId();
        reverse(profile, "ONLINE_ORDER", orderId, null, reason);
        restoreOnlineRedemption(profile, order, reason);
    }

    /** One idempotent proportional reversal/restoration for a completed after-sales refund. */
    @Transactional
    public void reverseForAfterSalesRefund(ServiceRequest request, BigDecimal refundAmount, BigDecimal originalSaleAmount) {
        if (request == null || request.getId() == null || refundAmount == null || originalSaleAmount == null || originalSaleAmount.signum() <= 0) return;
        CustomerProfile profile = profileForUser(request.getCustomerId());
        if (profile == null || transactions.findBySourceTypeAndSourceIdAndTransactionType("AFTER_SALES_REFUND", request.getId(), "REVERSE").isPresent()) return;
        String originalType = request.getOrderId() == null ? "OFFLINE_BILL" : "ONLINE_ORDER";
        Long originalId = request.getOrderId() == null ? request.getOfflineBillId() : request.getOrderId();
        LoyaltyPointTransaction earned = transactions.findBySourceTypeAndSourceIdAndTransactionType(originalType, originalId, "EARN").orElse(null);
        if (earned != null && earned.getPoints() > 0) {
            long reversal = refundAmount.multiply(BigDecimal.valueOf(earned.getPoints())).divide(originalSaleAmount, 0, RoundingMode.DOWN).longValue();
            if (reversal > 0) append(profile, "REVERSE", "AFTER_SALES_REFUND", request.getId(), request.getSellerId(), -reversal,
                    "Proportional loyalty reversal for after-sales request " + request.getRequestNumber());
        }
        LoyaltyPointTransaction redeemed = transactions.findBySourceTypeAndSourceIdAndTransactionType(originalType, originalId, "REDEEM").orElse(null);
        if (redeemed != null && redeemed.getPoints() < 0 && transactions.findBySourceTypeAndSourceIdAndTransactionType("AFTER_SALES_RESTORE", request.getId(), "ADJUST").isEmpty()) {
            long restore = refundAmount.multiply(BigDecimal.valueOf(Math.abs(redeemed.getPoints()))).divide(originalSaleAmount, 0, RoundingMode.DOWN).longValue();
            if (restore > 0) append(profile, "ADJUST", "AFTER_SALES_RESTORE", request.getId(), request.getSellerId(), restore,
                    "Proportional redeemed-points restoration for after-sales request " + request.getRequestNumber());
        }
    }

    @Transactional(readOnly = true)
    public RedemptionQuote quoteOfflineRedemption(Long customerId, String mobile, Long sellerId, Integer requestedPoints, BigDecimal saleTotal) {
        long requested = requestedPoints == null ? 0 : requestedPoints;
        if (requested < 0) throw new IllegalArgumentException("Loyalty points cannot be negative");
        if (requested == 0) return new RedemptionQuote(0, BigDecimal.ZERO, available(customerId, mobile));
        if (requested % POINTS_PER_RUPEE != 0) {
            throw new IllegalArgumentException("Redeem loyalty points in multiples of 3 (3 points = Rs. 1)");
        }
        CustomerProfile profile = customerId == null ? profileByMobile(mobile) : profileForUser(customerId);
        if (profile == null) throw new IllegalArgumentException("Loyalty redemption requires a registered customer");
        LoyaltySettings rule = ruleFor(sellerId);
        if (rule.getMaximumPointsPerSale() > 0 && requested > rule.getMaximumPointsPerSale())
            throw new IllegalArgumentException("Requested points exceed the maximum allowed for one bill");
        long available = walletRead(profile).getAvailablePoints();
        if (requested > available) throw new IllegalArgumentException("Customer does not have enough loyalty points");
        BigDecimal discount = redemptionValue(requested);
        if (saleTotal != null && discount.compareTo(saleTotal) > 0) throw new IllegalArgumentException("Points discount cannot exceed the bill total");
        return new RedemptionQuote(requested, discount, available);
    }

    /** Quote only; checkout still repeats this validation transactionally before an order is saved. */
    @Transactional(readOnly = true)
    public RedemptionQuote quoteOnlineRedemption(Long customerId, Integer requestedPoints, BigDecimal saleTotal) {
        long requested = requestedPoints == null ? 0 : requestedPoints;
        if (requested < 0) throw new IllegalArgumentException("Loyalty points cannot be negative");
        CustomerProfile profile = existingProfileForUser(customerId);
        if (requested == 0) return new RedemptionQuote(0, BigDecimal.ZERO, profile == null ? 0 : walletRead(profile).getAvailablePoints());
        if (profile == null) throw new IllegalArgumentException("Loyalty redemption requires a registered customer");
        if (requested % POINTS_PER_RUPEE != 0) throw new IllegalArgumentException("Redeem loyalty points in multiples of 3 (3 points = Rs. 1)");
        LoyaltySettings rule = ruleFor(null);
        if (rule.getMaximumPointsPerSale() > 0 && requested > rule.getMaximumPointsPerSale()) throw new IllegalArgumentException("Requested points exceed the maximum allowed for one order");
        long available = walletRead(profile).getAvailablePoints();
        if (requested > available) throw new IllegalArgumentException("Customer does not have enough loyalty points");
        BigDecimal discount = redemptionValue(requested);
        if (saleTotal != null && discount.compareTo(saleTotal) > 0) throw new IllegalArgumentException("Points discount cannot exceed the order total");
        return new RedemptionQuote(requested, discount, available);
    }

    /** Debits an accepted checkout once. It runs in the same transaction as the order save. */
    @Transactional
    public void redeemForOnlineOrder(Order order) {
        if (order == null || order.getId() == null || order.getLoyaltyPointsRedeemed() <= 0
                || transactions.findBySourceTypeAndSourceIdAndTransactionType("ONLINE_ORDER", order.getId(), "REDEEM").isPresent()) return;
        CustomerProfile profile = profileForUser(order.getCustomerId());
        if (profile == null) throw new IllegalArgumentException("Loyalty redemption requires a registered customer");
        long requested = order.getLoyaltyPointsRedeemed();
        if (requested % POINTS_PER_RUPEE != 0) throw new IllegalArgumentException("Redeem loyalty points in multiples of 3 (3 points = Rs. 1)");
        CustomerLoyaltyWallet wallet = walletForUpdate(profile);
        if (requested > wallet.getAvailablePoints()) throw new IllegalStateException("Loyalty balance changed before the order was placed");
        append(profile, "REDEEM", "ONLINE_ORDER", order.getId(), null, -requested,
                "Points redeemed on online order " + order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> customerLoyalty(Long customerId) {
        CustomerProfile profile = existingProfileForUser(customerId);
        if (profile == null) return Map.of("availablePoints", 0, "lifetimeEarnedPoints", 0, "lifetimeRedeemedPoints", 0, "transactions", List.of());
        CustomerLoyaltyWallet wallet = walletRead(profile);
        return summary(profile, wallet);
    }

    /** A GET must be idempotent: it reads an existing profile/wallet and never provisions either. */
    @Transactional(readOnly = true)
    public CustomerLoyaltySummaryResponse customerSummary(Long customerId) {
        CustomerProfile profile = existingProfileForUser(customerId);
        if (profile == null) return new CustomerLoyaltySummaryResponse(0, 0, 0, 0);
        CustomerLoyaltyWallet wallet = walletRead(profile);
        return new CustomerLoyaltySummaryResponse(wallet.getAvailablePoints(), wallet.getLifetimeEarnedPoints(), wallet.getLifetimeRedeemedPoints(), 0);
    }

    @Transactional(readOnly = true)
    public Page<CustomerLoyaltyTransactionResponse> customerTransactions(Long customerId, int page, int size) {
        CustomerProfile profile = existingProfileForUser(customerId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (profile == null) return Page.empty(pageable);
        return transactions.findByCustomerProfileId(profile.getId(), pageable).map(this::transactionResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loyaltyForMobile(String mobile, Long sellerId) {
        CustomerProfile profile = profileByMobile(mobile);
        if (profile == null) return Map.of("found", false, "availablePoints", 0);
        CustomerLoyaltyWallet wallet = walletRead(profile);
        Map<String, Object> result = new LinkedHashMap<>(summary(profile, wallet));
        result.put("found", true); result.put("sellerId", sellerId);
        return result;
    }

    @Transactional
    public LoyaltySettings saveSettings(LoyaltySettingsRequest request) {
        String scope = request.getSellerId() == null ? "GLOBAL" : "SELLER";
        LoyaltySettings item = request.getSellerId() == null
                ? settings.findFirstByScopeTypeAndSellerIdIsNullAndActiveTrue(scope).orElseGet(LoyaltySettings::new)
                : settings.findFirstByScopeTypeAndSellerIdAndActiveTrue(scope, request.getSellerId()).orElseGet(LoyaltySettings::new);
        item.setScopeType(scope); item.setSellerId(request.getSellerId()); item.setMinimumPurchaseAmount(request.getMinimumPurchaseAmount());
        item.setPointsPerPurchaseUnit(request.getPointsPerPurchaseUnit()); item.setPurchaseUnitInRupees(request.getPurchaseUnitInRupees());
        item.setPointValueInRupees(request.getPointValueInRupees()); item.setMaximumPointsPerSale(request.getMaximumPointsPerSale()); item.setActive(request.isActive());
        return settings.save(item);
    }

    @Transactional
    public Map<String, Object> adjust(LoyaltyAdjustmentRequest request, Long adminId) {
        CustomerProfile profile = profiles.findById(request.getCustomerProfileId()).orElseThrow(() -> new IllegalArgumentException("Customer profile not found"));
        if (request.getPoints() == 0) throw new IllegalArgumentException("Adjustment points cannot be zero");
        append(profile, "ADJUST", "ADMIN", System.nanoTime(), null, request.getPoints(), request.getReason().trim());
        return summary(profile, wallet(profile));
    }

    @Transactional(readOnly = true)
    public List<LoyaltySettings> settings() { return settings.findAll(); }

    public record RedemptionQuote(long points, BigDecimal discount, long availablePoints) { }

    private long redeemForOfflineBill(OfflineBill bill, CustomerProfile profile) {
        long requested = bill.getLoyaltyPointsRedeemed() == null ? 0 : bill.getLoyaltyPointsRedeemed();
        if (requested <= 0 || transactions.findBySourceTypeAndSourceIdAndTransactionType("OFFLINE_BILL", bill.getId(), "REDEEM").isPresent()) return 0;
        if (requested % POINTS_PER_RUPEE != 0) throw new IllegalArgumentException("Redeem loyalty points in multiples of 3 (3 points = Rs. 1)");
        CustomerLoyaltyWallet wallet = wallet(profile);
        if (requested > wallet.getAvailablePoints()) throw new IllegalStateException("Loyalty balance changed before bill payment was completed");
        append(profile, "REDEEM", "OFFLINE_BILL", bill.getId(), bill.getSellerId(), -requested, "Points redeemed on POS bill " + bill.getBillNumber());
        return requested;
    }

    private void restoreOnlineRedemption(CustomerProfile profile, Order order, String reason) {
        if (profile == null || order == null || order.getId() == null || order.getLoyaltyPointsRedeemed() <= 0
                || transactions.findBySourceTypeAndSourceIdAndTransactionType("ONLINE_ORDER", order.getId(), "ADJUST").isPresent()) return;
        LoyaltyPointTransaction redeemed = transactions.findBySourceTypeAndSourceIdAndTransactionType("ONLINE_ORDER", order.getId(), "REDEEM").orElse(null);
        if (redeemed != null && redeemed.getPoints() < 0) {
            append(profile, "ADJUST", "ONLINE_ORDER", order.getId(), null, Math.abs(redeemed.getPoints()),
                    "Redeemed points restored: " + (hasText(reason) ? reason : "Order cancelled or refunded"));
        }
    }

    private BigDecimal redemptionValue(long points) {
        return BigDecimal.valueOf(points / POINTS_PER_RUPEE).setScale(2, RoundingMode.HALF_UP);
    }

    /** Email is deferred until the ledger transaction commits and can never undo a paid sale. */
    private void scheduleLoyaltyEmail(CustomerProfile profile, String fallbackEmail, String fallbackName,
                                      String reference, long earnedPoints, long redeemedPoints,
                                      long availablePoints, BigDecimal redemptionDiscount) {
        String email = hasText(profile.getEmail()) ? profile.getEmail() : fallbackEmail;
        String name = hasText(profile.getName()) ? profile.getName() : fallbackName;
        if (!hasText(email)) return;
        Runnable sendNotification = () -> {
            try {
                emailService.sendLoyaltyPointsEmail(email, name, reference, earnedPoints, redeemedPoints,
                        availablePoints, redemptionDiscount);
            } catch (Exception ignored) {
                // A loyalty receipt is informational and must never roll back a completed payment.
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { sendNotification.run(); }
            });
        } else {
            sendNotification.run();
        }
    }

    private void reverse(CustomerProfile profile, String sourceType, Long sourceId, Long sellerId, String reason) {
        if (profile == null || sourceId == null || transactions.findBySourceTypeAndSourceIdAndTransactionType(sourceType, sourceId, "REVERSE").isPresent()) return;
        LoyaltyPointTransaction earn = transactions.findBySourceTypeAndSourceIdAndTransactionType(sourceType, sourceId, "EARN").orElse(null);
        if (earn != null && earn.getPoints() > 0) append(profile, "REVERSE", sourceType, sourceId, sellerId, -earn.getPoints(), reason == null ? "Sale cancelled or refunded" : reason);
    }

    private long earned(LoyaltySettings rule, BigDecimal amount) {
        if (amount == null || amount.compareTo(rule.getMinimumPurchaseAmount()) < 0) return 0;
        return amount.divide(rule.getPurchaseUnitInRupees(), 0, RoundingMode.DOWN).longValue() * rule.getPointsPerPurchaseUnit();
    }

    private LoyaltyPointTransaction append(CustomerProfile profile, String type, String sourceType, Long sourceId, Long sellerId, long points, String remarks) {
        CustomerLoyaltyWallet wallet = wallet(profile); long after = wallet.getAvailablePoints() + points;
        if (after < 0) throw new IllegalArgumentException("Loyalty balance cannot become negative");
        wallet.setAvailablePoints(after);
        if ("EARN".equals(type) && points > 0) wallet.setLifetimeEarnedPoints(wallet.getLifetimeEarnedPoints() + points);
        if ("REDEEM".equals(type) && points < 0) wallet.setLifetimeRedeemedPoints(wallet.getLifetimeRedeemedPoints() + Math.abs(points));
        wallets.save(wallet);
        LoyaltyPointTransaction tx = new LoyaltyPointTransaction(); tx.setCustomerProfile(profile); tx.setTransactionType(type); tx.setSourceType(sourceType);
        tx.setSourceId(sourceId); tx.setSellerId(sellerId); tx.setPoints(points); tx.setBalanceAfter(after); tx.setRemarks(remarks);
        return transactions.save(tx);
    }

    private CustomerLoyaltyWallet wallet(CustomerProfile profile) {
        return wallets.findByCustomerProfileId(profile.getId()).orElseGet(() -> { CustomerLoyaltyWallet item = new CustomerLoyaltyWallet(); item.setCustomerProfile(profile); return wallets.save(item); });
    }
    private CustomerLoyaltyWallet walletForUpdate(CustomerProfile profile) {
        return wallets.findByCustomerProfileIdForUpdate(profile.getId()).orElseThrow(() -> new IllegalStateException("Loyalty wallet was not found"));
    }
    private CustomerLoyaltyWallet walletRead(CustomerProfile profile) { return wallets.findByCustomerProfileId(profile.getId()).orElseGet(() -> { CustomerLoyaltyWallet item = new CustomerLoyaltyWallet(); item.setCustomerProfile(profile); return item; }); }
    private long available(Long customerId, String mobile) { CustomerProfile p = customerId == null ? profileByMobile(mobile) : profileForUser(customerId); return p == null ? 0 : walletRead(p).getAvailablePoints(); }
    private CustomerProfile existingProfileForUser(Long customerId) {
        if (customerId == null) return null;
        User user = users.findById(customerId).filter(item -> item.getRole() == Role.CUSTOMER).orElse(null);
        return user == null ? null : profiles.findByOnlineUser(user).orElse(null);
    }
    private CustomerProfile profileForUser(Long customerId) {
        if (customerId == null) return null; User user = users.findById(customerId).filter(item -> item.getRole() == Role.CUSTOMER).orElse(null); if (user == null) return null;
        return profiles.findByOnlineUser(user).orElseGet(() -> { CustomerProfile p = new CustomerProfile(); p.setOnlineUser(user); p.setName(user.getName()); p.setMobile(user.getMobile()); p.setEmail(user.getEmail()); p.setDateOfBirth(user.getDateOfBirth()); p.setCommunicationConsent(!user.isMarketingOptOut()); return profiles.save(p); });
    }
    private CustomerProfile profileByMobile(String mobile) { return mobile == null || mobile.isBlank() ? null : profiles.findFirstByMobile(mobile.trim()).orElse(null); }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private CustomerProfile profileForBill(OfflineBill bill) { if (bill == null) return null; CustomerProfile p = profileForUser(bill.getCustomerId()); return p != null ? p : profileByMobile(bill.getCustomerMobile()); }
    private LoyaltySettings ruleFor(Long sellerId) { if (sellerId != null) { LoyaltySettings seller = settings.findFirstByScopeTypeAndSellerIdAndActiveTrue("SELLER", sellerId).orElse(null); if (seller != null) return seller; } return settings.findFirstByScopeTypeAndSellerIdIsNullAndActiveTrue("GLOBAL").orElseGet(this::defaultRule); }
    private LoyaltySettings defaultRule() { LoyaltySettings item = new LoyaltySettings(); item.setScopeType("GLOBAL"); return item; }
    private CustomerLoyaltyTransactionResponse transactionResponse(LoyaltyPointTransaction transaction) { return new CustomerLoyaltyTransactionResponse(transaction.getId(), transaction.getTransactionType(), transaction.getSourceType(), transaction.getSourceId(), transaction.getPoints(), transaction.getBalanceAfter(), transaction.getRemarks(), transaction.getCreatedAt()); }
    private Map<String, Object> summary(CustomerProfile profile, CustomerLoyaltyWallet wallet) { Map<String, Object> result = new LinkedHashMap<>(); result.put("customerProfileId", profile.getId()); result.put("availablePoints", wallet.getAvailablePoints()); result.put("lifetimeEarnedPoints", wallet.getLifetimeEarnedPoints()); result.put("lifetimeRedeemedPoints", wallet.getLifetimeRedeemedPoints()); result.put("expiringPoints", 0); result.put("transactions", transactions.findByCustomerProfileIdOrderByCreatedAtDesc(profile.getId()).stream().map(this::transactionResponse).toList()); return result; }
}
