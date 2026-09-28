package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShopRegisterSummaryResponse(
        LocalDate businessDate, BigDecimal openingCash, BigDecimal cashSales,
        BigDecimal cashDeposits, BigDecimal cashExpenses, BigDecimal cashWithdrawals,
        BigDecimal cashAdjustments, BigDecimal expectedClosingCash,
        BigDecimal upiCollection, BigDecimal cardCollection, BigDecimal bankCollection,
        BigDecimal otherOnlineCollection, BigDecimal onlineCollection,
        BigDecimal totalExpense, BigDecimal netShopCollection, long entryCount) { }
