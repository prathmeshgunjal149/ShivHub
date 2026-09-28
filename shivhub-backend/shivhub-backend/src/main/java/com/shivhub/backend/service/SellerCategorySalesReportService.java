package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.SellerCategorySalesRow;
import com.shivhub.backend.dto.SellerProductSalesRow;

/** Category totals are calculated from the same seller-owned product sales rows. */
@Service
public class SellerCategorySalesReportService {
    private final SellerProductSalesReportService productSales;
    public SellerCategorySalesReportService(SellerProductSalesReportService productSales) { this.productSales = productSales; }
    @Transactional(readOnly = true)
    public List<SellerCategorySalesRow> get(String email, LocalDate start, LocalDate end) {
        Map<String, Totals> totals = new LinkedHashMap<>();
        for (SellerProductSalesRow row : productSales.get(email, start, end)) {
            String category = row.category() == null || row.category().isBlank() ? "Uncategorised" : row.category();
            Totals total = totals.computeIfAbsent(category, ignored -> new Totals());
            total.online += row.onlineQuantity(); total.pos += row.posQuantity(); total.sales = total.sales.add(row.totalSales());
        }
        return totals.entrySet().stream().map(entry -> new SellerCategorySalesRow(entry.getKey(), entry.getValue().online, entry.getValue().pos, entry.getValue().online + entry.getValue().pos, entry.getValue().sales)).sorted((a,b) -> b.totalSales().compareTo(a.totalSales())).toList();
    }
    private static class Totals { long online; long pos; BigDecimal sales = BigDecimal.ZERO; }
}
