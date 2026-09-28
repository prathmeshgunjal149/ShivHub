package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SellerOperationsReportResponse(List<PaymentRow> payments, List<GstRow> gst,
        List<CustomerRow> customers, List<PurchaseRow> purchases, List<StockRow> stock,
        List<SerialRow> serials) {
    public record PaymentRow(String method, long billCount, BigDecimal amount) {}
    public record GstRow(String source, BigDecimal taxableAmount, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal discount, BigDecimal total) {}
    public record CustomerRow(String customer, String mobile, long billCount, BigDecimal totalSpent, LocalDateTime lastPurchaseAt) {}
    public record PurchaseRow(Long purchaseId, String invoiceNumber, String distributor, LocalDateTime purchaseDate, String status, BigDecimal total, String invoiceFileUrl) {}
    public record StockRow(Long productId, String productName, String category, Integer stock, boolean lowStock) {}
    public record SerialRow(Long serialId, String productName, String imei1, String imei2, String serialNumber, String status, LocalDateTime soldAt, String billNumber) {}
}
