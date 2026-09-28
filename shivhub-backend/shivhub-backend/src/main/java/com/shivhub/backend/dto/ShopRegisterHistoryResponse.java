package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ShopRegisterHistoryResponse(
        LocalDate businessDate, BigDecimal openingCash, BigDecimal expectedClosingCash,
        BigDecimal cashSales, BigDecimal onlineCollection, BigDecimal totalExpense, long entryCount) { }
