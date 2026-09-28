package com.shivhub.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.config.RazorpayConfig;
import com.shivhub.backend.dto.RazorpayVerificationRequest;
import com.shivhub.backend.dto.ManualPaymentRequest;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.PaymentTransaction;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.*;
import com.shivhub.backend.repository.*;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceTest {
    @Mock private RazorpayConfig config;
    @Mock private PaymentTransactionRepository transactions;
    @Mock private PaymentWebhookEventRepository webhookEvents;
    @Mock private OrderRepository orders;
    @Mock private OfflineBillRepository bills;
    @Mock private UserRepository users;
    @Mock private EmailService emailService;
    @Mock private OfflineBillPaymentFulfillmentService offlineBillFulfillment;
    @Mock private OfflineBillInvoiceService offlineBillInvoices;
    @Mock private LoyaltyService loyaltyService;
    private RazorpayPaymentService service;
    private final String secret = "test_webhook_secret";

    @BeforeEach void setUp() {
        lenient().when(config.getWebhookSecret()).thenReturn(secret);
        lenient().when(config.getKeySecret()).thenReturn(secret);
        service = new RazorpayPaymentService(config, transactions, webhookEvents, orders, bills, users,
                new ObjectMapper(), emailService, offlineBillFulfillment, offlineBillInvoices, loyaltyService);
    }

    @Test void acceptsOnlyAValidWebhookHmac() throws Exception {
        String body = "{\"event\":\"payment.captured\"}";
        assertTrue(service.validWebhookSignature(body, hmac(body)));
        assertFalse(service.validWebhookSignature(body, "not-a-valid-signature"));
    }

    @Test void validCheckoutSignatureMarksTheOwnedOrderPaidAndConfirmed() throws Exception {
        User customer = customer(); Order order = order(); PaymentTransaction transaction = transaction(PaymentStatus.CREATED);
        when(users.findByEmail("customer@shivhub.test")).thenReturn(Optional.of(customer));
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(transactions.findByRazorpayOrderId("order_test_1")).thenReturn(Optional.of(transaction));
        when(transactions.findByRazorpayPaymentId("pay_test_1")).thenReturn(Optional.empty());
        RazorpayVerificationRequest request = request("pay_test_1", hmac("order_test_1|pay_test_1"));

        assertTrue(service.verifyOnline(request, "customer@shivhub.test").isSuccess());
        assertEquals(PaymentStatus.PAID, transaction.getPaymentStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(transactions, atLeastOnce()).save(transaction);
        verify(orders, atLeastOnce()).save(order);
    }

    @Test void invalidCheckoutSignatureNeverConfirmsTheOrder() {
        User customer = customer(); Order order = order(); PaymentTransaction transaction = transaction(PaymentStatus.CREATED);
        when(users.findByEmail("customer@shivhub.test")).thenReturn(Optional.of(customer));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(transactions.findByRazorpayOrderId("order_test_1")).thenReturn(Optional.of(transaction));

        assertThrows(SecurityException.class, () -> service.verifyOnline(request("pay_test_1", "wrong"), "customer@shivhub.test"));
        assertEquals(PaymentStatus.CREATED, transaction.getPaymentStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        verify(orders, never()).save(any());
    }

    @Test void repeatedVerificationDoesNotSettleStockOrOrderTwice() throws Exception {
        User customer = customer(); Order order = order(); order.setPaymentStatus(PaymentStatus.PAID); order.setOrderStatus(OrderStatus.CONFIRMED);
        PaymentTransaction transaction = transaction(PaymentStatus.PAID); transaction.setRazorpayPaymentId("pay_test_1"); transaction.setSettledAt(LocalDateTime.now());
        when(users.findByEmail("customer@shivhub.test")).thenReturn(Optional.of(customer));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(transactions.findByRazorpayOrderId("order_test_1")).thenReturn(Optional.of(transaction));

        assertTrue(service.verifyOnline(request("pay_test_1", hmac("order_test_1|pay_test_1")), "customer@shivhub.test").isSuccess());
        verify(orders, never()).save(any());
    }

    @Test void sellerCashCollectionCreatesAnAuditablePartialPayment() {
        User seller = new User(); seller.setId(9L); seller.setRole(Role.SELLER);
        OfflineBill bill = new OfflineBill(); bill.setId(31L); bill.setSellerId(9L);
        bill.setGrandTotal(new BigDecimal("100.00")); bill.setPaymentAmount(BigDecimal.ZERO);
        bill.setPaymentStatus(PaymentStatus.PENDING);
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setPaymentMethod(PaymentMethod.CASH); request.setAmount(new BigDecimal("45.00"));
        request.setTransactionReference("COUNTER-45");

        when(users.findByEmail("seller@shivhub.test")).thenReturn(Optional.of(seller));
        when(bills.findById(31L)).thenReturn(Optional.of(bill));

        service.recordManualOfflinePayment(31L, request, "seller@shivhub.test");

        assertEquals(new BigDecimal("45.00"), bill.getPaymentAmount());
        assertEquals(PaymentStatus.PARTIALLY_PAID, bill.getPaymentStatus());
        verify(transactions).save(any(PaymentTransaction.class));
    }

    @Test void sellerCannotRecordAPaymentAgainstAnotherSellersBill() {
        User seller = new User(); seller.setId(9L); seller.setRole(Role.SELLER);
        OfflineBill bill = new OfflineBill(); bill.setId(31L); bill.setSellerId(10L);
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setPaymentMethod(PaymentMethod.CASH); request.setAmount(BigDecimal.ONE);
        when(users.findByEmail("seller@shivhub.test")).thenReturn(Optional.of(seller));
        when(bills.findById(31L)).thenReturn(Optional.of(bill));

        assertThrows(SecurityException.class, () -> service.recordManualOfflinePayment(31L, request, "seller@shivhub.test"));
        verifyNoInteractions(transactions);
    }

    @Test void verifiedSellerRazorpayPaymentFinalizesPreviouslyReservedInventory() throws Exception {
        User seller = new User(); seller.setId(9L); seller.setRole(Role.SELLER);
        OfflineBill bill = new OfflineBill(); bill.setId(31L); bill.setSellerId(9L);
        bill.setGrandTotal(new BigDecimal("100.00")); bill.setPaymentAmount(BigDecimal.ZERO);
        bill.setPaymentStatus(PaymentStatus.PENDING); bill.setInventoryPending(true);
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(20L); transaction.setPaymentContext(PaymentContext.OFFLINE_BILL);
        transaction.setPaymentMethod(PaymentMethod.RAZORPAY); transaction.setPaymentStatus(PaymentStatus.CREATED);
        transaction.setOfflineBillId(31L); transaction.setAmount(new BigDecimal("100.00"));
        transaction.setRazorpayOrderId("order_offline_1");
        RazorpayVerificationRequest request = new RazorpayVerificationRequest();
        request.setRazorpayOrderId("order_offline_1"); request.setRazorpayPaymentId("pay_offline_1");
        request.setRazorpaySignature(hmac("order_offline_1|pay_offline_1"));

        when(users.findByEmail("seller@shivhub.test")).thenReturn(Optional.of(seller));
        when(bills.findById(31L)).thenReturn(Optional.of(bill));
        when(transactions.findByRazorpayOrderId("order_offline_1")).thenReturn(Optional.of(transaction));
        when(transactions.findByRazorpayPaymentId("pay_offline_1")).thenReturn(Optional.empty());
        when(transactions.findByOfflineBillIdOrderByCreatedAtDesc(31L)).thenReturn(java.util.List.of(transaction));

        service.verifyOffline(31L, request, "seller@shivhub.test");

        assertEquals(PaymentStatus.PAID, bill.getPaymentStatus());
        assertEquals(new BigDecimal("100.00"), bill.getPaymentAmount());
        verify(offlineBillFulfillment).finalizeIfPending(bill);
    }

    @Test void duplicateWebhookEventDoesNotConfirmTheOrderTwice() {
        Order order = order(); PaymentTransaction transaction = transaction(PaymentStatus.CREATED);
        String body = "{\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_test_1\",\"order_id\":\"order_test_1\",\"amount\":10000,\"currency\":\"INR\"}}}}";
        when(webhookEvents.existsByGatewayEventId("evt_1")).thenReturn(false, true);
        when(transactions.findByRazorpayOrderId("order_test_1")).thenReturn(Optional.of(transaction));
        when(transactions.findByRazorpayPaymentId("pay_test_1")).thenReturn(Optional.empty());
        when(orders.findById(11L)).thenReturn(Optional.of(order));

        service.processWebhook("payment.captured", body, "evt_1");
        service.processWebhook("payment.captured", body, "evt_1");

        assertEquals(PaymentStatus.PAID, transaction.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(orders, times(1)).save(order);
        verify(webhookEvents, times(1)).save(any());
    }

    private User customer() { User value = new User(); value.setId(7L); value.setName("Customer"); value.setEmail("customer@shivhub.test"); value.setRole(Role.CUSTOMER); return value; }
    private Order order() { Order value = new Order(); value.setId(11L); value.setCustomerId(7L); value.setOrderNumber("SH-1001"); value.setGrandTotal(new BigDecimal("100.00")); value.setOrderStatus(OrderStatus.PENDING); value.setPaymentStatus(PaymentStatus.PENDING); return value; }
    private PaymentTransaction transaction(PaymentStatus status) { PaymentTransaction value = new PaymentTransaction(); value.setId(15L); value.setPaymentContext(PaymentContext.ONLINE_ORDER); value.setPaymentMethod(PaymentMethod.RAZORPAY); value.setPaymentStatus(status); value.setOrderId(11L); value.setAmount(new BigDecimal("100.00")); value.setRazorpayOrderId("order_test_1"); return value; }
    private RazorpayVerificationRequest request(String paymentId, String signature) { RazorpayVerificationRequest value = new RazorpayVerificationRequest(); value.setInternalOrderId(11L); value.setRazorpayOrderId("order_test_1"); value.setRazorpayPaymentId(paymentId); value.setRazorpaySignature(signature); return value; }
    private String hmac(String payload) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))); }
}
