package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shivhub.backend.entity.CouponDiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CouponRequest {
    @NotBlank private String code;
    @NotBlank private String title;
    private String description;
    @NotNull private CouponDiscountType discountType;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal discountValue;
    @DecimalMin(value = "0.00") private BigDecimal minimumOrderAmount = BigDecimal.ZERO;
    @DecimalMin(value = "0.01") private BigDecimal maximumDiscount;
    @Positive private Integer usageLimit;
    @Positive private Integer perCustomerLimit = 1;
    private boolean active = true;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
}
