package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.entity.CouponDiscountType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    private CouponDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer perCustomerLimit;
    private boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private BigDecimal discountAmount;
}
