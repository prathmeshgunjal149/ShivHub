package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class GstCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    public GstBreakup inclusive(BigDecimal inclusiveAmount, BigDecimal gstRate) {
        return inclusive(inclusiveAmount, gstRate, false);
    }

    public GstBreakup inclusive(BigDecimal inclusiveAmount, BigDecimal gstRate, boolean interstate) {
        BigDecimal finalAmount = money(inclusiveAmount);
        BigDecimal rate = rate(gstRate);
        if (finalAmount.signum() <= 0 || rate.signum() == 0) {
            return new GstBreakup(finalAmount, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                    BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), finalAmount);
        }

        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(HUNDRED, 8, RoundingMode.HALF_UP));
        BigDecimal taxable = finalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal totalGst = finalAmount.subtract(taxable).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = BigDecimal.ZERO.setScale(2);
        BigDecimal sgst = BigDecimal.ZERO.setScale(2);
        BigDecimal igst = BigDecimal.ZERO.setScale(2);

        if (interstate) {
            igst = totalGst;
        } else {
            cgst = totalGst.divide(TWO, 2, RoundingMode.HALF_UP);
            sgst = totalGst.subtract(cgst).setScale(2, RoundingMode.HALF_UP);
        }

        return new GstBreakup(taxable, cgst, sgst, igst, totalGst, finalAmount);
    }

    public BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal rate(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw new RuntimeException("GST rate must be between 0 and 100");
        }
        return value;
    }
}
