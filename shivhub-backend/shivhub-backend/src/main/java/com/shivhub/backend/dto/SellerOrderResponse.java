package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.PaymentStatus;

/** Seller-safe order fulfilment response. Totals/items are only for that seller. */
public record SellerOrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        String customerEmail,
        String customerMobile,
        String shippingAddress,
        OrderStatus orderStatus,
        OrderStatus sellerRequestedStatus,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        int itemCount,
        BigDecimal sellerTotal,
        String deliveryPersonName,
        String deliveryPersonMobile,
        LocalDateTime expectedDeliveryAt,
        String expectedDeliveryMessage,
        LocalDateTime expectedDeliveryUpdatedAt,
        boolean invoiceAvailable,
        List<Item> items
) {

    public record Item(
            Long orderItemId,
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String assignedSerialSummary,
            List<SerialOption> availableSerials,
            List<SerialOption> selectedSerials
    ) {}

    public record SerialOption(
            Long serialId,
            String imei1,
            String imei2,
            String serialNumber,
            String display,
            String status
    ) {}
}
