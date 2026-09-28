package com.shivhub.backend.service;

import java.math.BigDecimal;

public record GstBreakup(
        BigDecimal taxableAmount,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal totalGst,
        BigDecimal finalAmount
) {
}
