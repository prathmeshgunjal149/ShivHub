package com.shivhub.backend.dto;
import java.math.BigDecimal;
public record DeliveryEstimateResponse(boolean applicable,boolean locationAvailable,Boolean serviceAvailable,
    String message,String estimatedDeliveryText,BigDecimal distanceKm,String distanceMethod,boolean estimated,
    String sellerShopName,Long sellerId,Long ruleId,Integer estimatedMinutes,BigDecimal deliveryCharge) { }
