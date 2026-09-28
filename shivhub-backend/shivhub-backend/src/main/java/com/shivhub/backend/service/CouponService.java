package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.CouponRequest;
import com.shivhub.backend.dto.CouponResponse;
import com.shivhub.backend.entity.Coupon;
import com.shivhub.backend.entity.CouponDiscountType;
import com.shivhub.backend.repository.CouponRepository;
import com.shivhub.backend.repository.CouponUsageRepository;

@Service
public class CouponService {
    private final CouponRepository coupons;
    private final CouponUsageRepository usages;

    public CouponService(CouponRepository coupons, CouponUsageRepository usages) {
        this.coupons = coupons;
        this.usages = usages;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> list() {
        return coupons.findAll().stream().map(coupon -> response(coupon, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> availableFor(Long customerId, BigDecimal subtotal) {
        return coupons.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(coupon -> coupon.getTargetCustomerId() == null || coupon.getTargetCustomerId().equals(customerId))
                .filter(coupon -> isWithinDateRange(coupon, LocalDateTime.now()))
                .filter(coupon -> subtotal == null || meetsMinimum(coupon, subtotal))
                .filter(coupon -> canCustomerUse(coupon, customerId))
                .map(coupon -> response(coupon, subtotal == null ? null : calculateDiscount(coupon, subtotal)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse validate(String code, Long customerId, BigDecimal subtotal) {
        Coupon coupon = findAndValidate(code, customerId, subtotal);
        return response(coupon, calculateDiscount(coupon, subtotal));
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = normalizeCode(request.getCode());
        if (coupons.existsByCodeIgnoreCase(code)) throw new RuntimeException("Coupon code already exists");
        Coupon coupon = new Coupon();
        apply(coupon, request, code);
        return response(coupons.save(coupon), null);
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = getCoupon(id);
        String code = normalizeCode(request.getCode());
        coupons.findByCodeIgnoreCase(code).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new RuntimeException("Coupon code already exists"); });
        apply(coupon, request, code);
        return response(coupons.save(coupon), null);
    }

    @Transactional
    public void delete(Long id) {
        coupons.delete(getCoupon(id));
    }

    /** Revalidates and records a redemption inside the order transaction. */
    @Transactional
    public Coupon applyToOrder(String code, Long customerId, BigDecimal subtotal) {
        Coupon coupon = findAndValidate(code, customerId, subtotal);
        coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
        return coupons.save(coupon);
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = coupon.getDiscountType() == CouponDiscountType.FIXED
                ? coupon.getDiscountValue()
                : subtotal.multiply(coupon.getDiscountValue()).movePointLeft(2);
        if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
            discount = coupon.getMaximumDiscount();
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private Coupon findAndValidate(String code, Long customerId, BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0) throw new RuntimeException("Cart subtotal must be greater than zero");
        Coupon coupon = coupons.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new RuntimeException("Invalid coupon code"));
        if (!coupon.isActive()) throw new RuntimeException("This coupon is inactive");
        if (coupon.getTargetCustomerId() != null && !coupon.getTargetCustomerId().equals(customerId)) throw new RuntimeException("This coupon belongs to another customer");
        if (!isWithinDateRange(coupon, LocalDateTime.now())) throw new RuntimeException("This coupon is not currently valid");
        if (!meetsMinimum(coupon, subtotal)) throw new RuntimeException("Minimum order amount for this coupon is " + coupon.getMinimumOrderAmount());
        if (coupon.getUsageLimit() != null && (coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) >= coupon.getUsageLimit()) {
            throw new RuntimeException("This coupon has reached its usage limit");
        }
        if (!canCustomerUse(coupon, customerId)) throw new RuntimeException("You have already used this coupon");
        return coupon;
    }

    private boolean canCustomerUse(Coupon coupon, Long customerId) {
        return customerId == null || coupon.getPerCustomerLimit() == null
                || usages.countByCouponIdAndCustomerId(coupon.getId(), customerId) < coupon.getPerCustomerLimit();
    }

    private boolean meetsMinimum(Coupon coupon, BigDecimal subtotal) {
        return coupon.getMinimumOrderAmount() == null || subtotal.compareTo(coupon.getMinimumOrderAmount()) >= 0;
    }

    private boolean isWithinDateRange(Coupon coupon, LocalDateTime now) {
        return (coupon.getStartsAt() == null || !now.isBefore(coupon.getStartsAt()))
                && (coupon.getEndsAt() == null || !now.isAfter(coupon.getEndsAt()));
    }

    private Coupon getCoupon(Long id) {
        return coupons.findById(id).orElseThrow(() -> new RuntimeException("Coupon not found"));
    }

    private void apply(Coupon coupon, CouponRequest request, String code) {
        if (request.getDiscountType() == CouponDiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Percentage discount cannot exceed 100");
        }
        LocalDateTime startsAt = request.getStartsAt() == null ? LocalDateTime.now() : request.getStartsAt();
        if (request.getEndsAt() != null && request.getEndsAt().isBefore(startsAt)) {
            throw new RuntimeException("Coupon end date must be after its start date");
        }
        coupon.setCode(code);
        coupon.setTitle(request.getTitle().trim());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount() == null ? BigDecimal.ZERO : request.getMinimumOrderAmount());
        coupon.setMaximumDiscount(request.getMaximumDiscount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerCustomerLimit(request.getPerCustomerLimit());
        coupon.setActive(request.isActive());
        coupon.setStartsAt(startsAt);
        coupon.setEndsAt(request.getEndsAt());
    }

    private CouponResponse response(Coupon coupon, BigDecimal discount) {
        return CouponResponse.builder().id(coupon.getId()).code(coupon.getCode()).title(coupon.getTitle())
                .description(coupon.getDescription()).discountType(coupon.getDiscountType()).discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount()).maximumDiscount(coupon.getMaximumDiscount())
                .usageLimit(coupon.getUsageLimit()).usedCount(coupon.getUsedCount()).perCustomerLimit(coupon.getPerCustomerLimit())
                .active(coupon.isActive()).startsAt(coupon.getStartsAt()).endsAt(coupon.getEndsAt()).discountAmount(discount).build();
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) throw new RuntimeException("Coupon code is required");
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
