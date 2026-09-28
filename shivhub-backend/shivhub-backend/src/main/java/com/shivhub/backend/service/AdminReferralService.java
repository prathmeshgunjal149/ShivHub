package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.AdminReferralResponse;
import com.shivhub.backend.entity.Coupon;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.Referral;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.CouponRepository;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.ReferralRepository;
import com.shivhub.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Read-only reporting over the existing referral lifecycle tables. */
@Service
@RequiredArgsConstructor
public class AdminReferralService {
    private final ReferralRepository referrals;
    private final UserRepository users;
    private final CouponRepository coupons;
    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public AdminReferralResponse list(String query, String status, LocalDate from, LocalDate to, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.min(100, Math.max(1, requestedSize));
        List<Referral> filtered = filtered(query, status, from, to);

        long total = filtered.size();
        int fromIndex = Math.min((int) Math.min((long) page * size, total), (int) total);
        int toIndex = Math.min(fromIndex + size, (int) total);
        List<AdminReferralResponse.Row> content = filtered.subList(fromIndex, toIndex).stream().map(this::row).toList();
        long pending = filtered.stream().filter(referral -> "PENDING".equals(statusOf(referral))).count();
        long rewarded = filtered.stream().filter(referral -> "REWARDED".equals(statusOf(referral))).count();
        long registered = filtered.stream().filter(referral -> "REGISTERED".equals(statusOf(referral))).count();

        return new AdminReferralResponse(
                content, total, (int) Math.ceil(total / (double) size), page, size,
                new AdminReferralResponse.Summary(total, pending, rewarded, registered),
                topReferrers(filtered));
    }

    @Transactional(readOnly = true)
    public List<AdminReferralResponse.Row> export(String query, String status, LocalDate from, LocalDate to) {
        return filtered(query, status, from, to).stream().map(this::row).toList();
    }

    private List<Referral> filtered(String query, String status, LocalDate from, LocalDate to) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.atTime(LocalTime.MAX);
        return referrals.findAllByOrderByCreatedAtDesc().stream()
                .filter(referral -> normalizedStatus.isBlank() || normalizedStatus.equals(statusOf(referral)))
                .filter(referral -> start == null || !referral.getCreatedAt().isBefore(start))
                .filter(referral -> end == null || !referral.getCreatedAt().isAfter(end))
                .filter(referral -> normalizedQuery.isBlank() || matches(referral, normalizedQuery))
                .toList();
    }

    private boolean matches(Referral referral, String query) {
        User referrer = user(referral.getReferrerCustomerId());
        User referred = user(referral.getReferredCustomerId());
        return List.of(
                value(referrer == null ? null : referrer.getName()), value(referrer == null ? null : referrer.getEmail()), value(referrer == null ? null : referrer.getMobile()),
                value(referred == null ? null : referred.getName()), value(referred == null ? null : referred.getEmail()), value(referred == null ? null : referred.getMobile()),
                value(referrer == null ? null : referrer.getReferralCode()), value(referral.getReferrerCouponCode()), value(referral.getReferredCouponCode()))
                .stream().anyMatch(value -> value.contains(query));
    }

    private AdminReferralResponse.Row row(Referral referral) {
        User referrer = user(referral.getReferrerCustomerId());
        User referred = user(referral.getReferredCustomerId());
        Order order = referral.getQualifyingOrderId() == null ? null : orders.findById(referral.getQualifyingOrderId()).orElse(null);
        return new AdminReferralResponse.Row(
                referral.getId(), customer(referrer), customer(referred),
                referrer == null ? null : referrer.getReferralCode(), referral.getCreatedAt(), statusOf(referral),
                coupon(referral.getReferrerCouponCode()), coupon(referral.getReferredCouponCode()),
                order == null ? null : order.getOrderNumber(), order == null ? null : order.getGrandTotal());
    }

    private List<AdminReferralResponse.TopReferrer> topReferrers(List<Referral> referralsToReport) {
        Map<Long, List<Referral>> byCustomer = referralsToReport.stream().collect(Collectors.groupingBy(Referral::getReferrerCustomerId));
        return byCustomer.entrySet().stream()
                .map(entry -> {
                    User user = user(entry.getKey());
                    long rewarded = entry.getValue().stream().filter(referral -> "REWARDED".equals(statusOf(referral))).count();
                    return new AdminReferralResponse.TopReferrer(entry.getKey(), user == null ? "Unknown customer" : user.getName(),
                            user == null ? null : user.getEmail(), user == null ? null : user.getMobile(), entry.getValue().size(), rewarded);
                })
                .sorted(Comparator.comparingLong(AdminReferralResponse.TopReferrer::referralCount).reversed())
                .limit(8)
                .toList();
    }

    private AdminReferralResponse.Customer customer(User user) {
        return user == null ? new AdminReferralResponse.Customer(null, "Unknown customer", null, null)
                : new AdminReferralResponse.Customer(user.getId(), user.getName(), user.getEmail(), user.getMobile());
    }

    private AdminReferralResponse.Coupon coupon(String code) {
        if (code == null || code.isBlank()) return new AdminReferralResponse.Coupon(null, false, false, 0);
        Optional<Coupon> coupon = coupons.findByCodeIgnoreCase(code);
        return coupon.map(value -> new AdminReferralResponse.Coupon(value.getCode(), true, value.getUsedCount() > 0, value.getUsedCount()))
                .orElseGet(() -> new AdminReferralResponse.Coupon(code, false, false, 0));
    }

    private User user(Long id) { return id == null ? null : users.findById(id).orElse(null); }
    private String statusOf(Referral referral) { return referral.getStatus() == null || referral.getStatus().isBlank() ? "PENDING" : referral.getStatus().toUpperCase(Locale.ROOT); }
    private String value(String source) { return source == null ? "" : source.toLowerCase(Locale.ROOT); }
}
