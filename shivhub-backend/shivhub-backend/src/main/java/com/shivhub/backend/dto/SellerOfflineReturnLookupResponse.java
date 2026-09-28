package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.shivhub.backend.enums.PaymentStatus;
import lombok.Data;

/** Seller-scoped POS return lookup. It exposes only a bill owned by the authenticated seller. */
@Data
public class SellerOfflineReturnLookupResponse {
    private Long billId;
    private String billNumber;
    private String customerName;
    private String customerMobile;
    private PaymentStatus paymentStatus;
    private LocalDateTime saleDate;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long billItemId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal refundableAmount;
        private boolean returnEligible;
        private String ineligibleReason;
        private List<Serial> serials = new ArrayList<>();
    }

    @Data
    public static class Serial {
        private Long purchaseSerialId;
        private String imei1;
        private String imei2;
        private String serialNumber;
    }
}
