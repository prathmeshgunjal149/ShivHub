package com.shivhub.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.Coupon;
import com.shivhub.backend.entity.CouponDiscountType;
import com.shivhub.backend.entity.MarketingCampaign;
import com.shivhub.backend.repository.CouponRepository;
import com.shivhub.backend.repository.MarketingCampaignRepository;

/**
 * Preserves coupons created through the earlier Marketing screen while the
 * checkout moves to the dedicated Coupon module. Existing codes are copied
 * only when they do not already exist, so this runner is safe on every start.
 */
@Component
public class LegacyMarketingCouponMigration implements ApplicationRunner {
    private final MarketingCampaignRepository marketingCampaigns;
    private final CouponRepository coupons;

    public LegacyMarketingCouponMigration(MarketingCampaignRepository marketingCampaigns, CouponRepository coupons) {
        this.marketingCampaigns = marketingCampaigns;
        this.coupons = coupons;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        marketingCampaigns.findByTypeOrderByCreatedAtDesc("COUPON").stream()
                .filter(item -> item.getCouponCode() != null && !item.getCouponCode().isBlank())
                .filter(item -> !coupons.existsByCodeIgnoreCase(item.getCouponCode().trim()))
                .forEach(this::copyCoupon);
    }

    private void copyCoupon(MarketingCampaign item) {
        BigDecimal discount = item.getDiscountValue() != null ? item.getDiscountValue() : item.getDiscountPercent();
        if (discount == null || discount.signum() <= 0) return;

        Coupon coupon = new Coupon();
        coupon.setCode(item.getCouponCode().trim().toUpperCase());
        coupon.setTitle(item.getTitle());
        coupon.setDescription(item.getDescription());
        coupon.setDiscountType(item.getCouponDiscountType() == null ? CouponDiscountType.PERCENTAGE : item.getCouponDiscountType());
        coupon.setDiscountValue(discount);
        coupon.setMinimumOrderAmount(item.getMinimumOrderAmount() == null ? BigDecimal.ZERO : item.getMinimumOrderAmount());
        coupon.setMaximumDiscount(item.getMaximumDiscount());
        coupon.setUsageLimit(item.getUsageLimit());
        coupon.setPerCustomerLimit(item.getPerCustomerLimit());
        coupon.setUsedCount(item.getUsedCount() == null ? 0 : item.getUsedCount());
        coupon.setActive(item.isActive());
        coupon.setStartsAt(item.getStartsAt());
        coupon.setEndsAt(item.getEndsAt());
        coupons.save(coupon);
    }
}
