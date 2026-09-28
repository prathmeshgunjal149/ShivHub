package com.shivhub.backend.controller;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shivhub.backend.dto.SellerOrderResponse;
import com.shivhub.backend.dto.OrderDeliveryExpectationRequest;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.service.SellerOrderService;

/** Seller order queue endpoint. Identity always comes from JWT. */
@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {
    private final SellerOrderService sellerOrderService;
    public SellerOrderController(SellerOrderService sellerOrderService) { this.sellerOrderService = sellerOrderService; }
    @GetMapping
    public ResponseEntity<List<SellerOrderResponse>> getMyOrders(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Authentication required");
        return ResponseEntity.ok(sellerOrderService.getMyOrders(authentication.getName()));
    }
    @PutMapping("/{orderId}/status-request")
    public ResponseEntity<Void> requestStatus(Authentication authentication, @PathVariable Long orderId, @RequestBody com.shivhub.backend.dto.UpdateOrderStatusRequest request) {
        if (authentication == null || authentication.getName() == null || request == null || request.getOrderStatus() == null) return ResponseEntity.badRequest().build();
        sellerOrderService.requestStatusChange(authentication.getName(), orderId, request.getOrderStatus(), request.getDeliveryPersonName(), request.getDeliveryPersonMobile(), request.getSerialSelections());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orderId}/delivery-expectation")
    public ResponseEntity<SellerOrderResponse> updateDeliveryExpectation(Authentication authentication, @PathVariable Long orderId,
            @jakarta.validation.Valid @RequestBody OrderDeliveryExpectationRequest request) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Authentication required");
        return ResponseEntity.ok(sellerOrderService.updateDeliveryExpectation(authentication.getName(), orderId, request));
    }

    @GetMapping(value = "/{orderId}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoice(Authentication authentication, @PathVariable Long orderId) {
        if (authentication == null || authentication.getName() == null) throw new RuntimeException("Authentication required");
        byte[] pdf = sellerOrderService.generateSellerInvoice(authentication.getName(), orderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=online-order-" + orderId + "-invoice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<SellerOrderResponse> updatePaymentStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody PaymentStatusRequest request) {

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }

        if (request == null || request.getPaymentStatus() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                sellerOrderService.updatePaymentStatus(
                        authentication.getName(),
                        orderId,
                        request.getPaymentStatus()
                )
        );
    }

    public static class PaymentStatusRequest {
        private PaymentStatus paymentStatus;

        public PaymentStatus getPaymentStatus() {
            return paymentStatus;
        }

        public void setPaymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
        }
    }
}
