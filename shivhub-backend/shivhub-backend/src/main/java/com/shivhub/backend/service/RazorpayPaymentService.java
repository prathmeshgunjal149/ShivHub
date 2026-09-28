package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.config.RazorpayConfig;
import com.shivhub.backend.dto.ManualPaymentRequest;
import com.shivhub.backend.dto.PaymentSummaryResponse;
import com.shivhub.backend.dto.PaymentTransactionResponse;
import com.shivhub.backend.dto.RazorpayOrderResponse;
import com.shivhub.backend.dto.RazorpayPaymentLinkResponse;
import com.shivhub.backend.dto.RazorpayVerificationRequest;
import com.shivhub.backend.dto.RazorpayVerificationResponse;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.PaymentTransaction;
import com.shivhub.backend.entity.PaymentWebhookEvent;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.PaymentContext;
import com.shivhub.backend.enums.PaymentMethod;
import com.shivhub.backend.enums.PaymentStatus;
import com.shivhub.backend.enums.OrderStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.OrderRepository;
import com.shivhub.backend.repository.PaymentTransactionRepository;
import com.shivhub.backend.repository.PaymentWebhookEventRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class RazorpayPaymentService {
    private final RazorpayConfig razorpay;
    private final PaymentTransactionRepository transactions;
    private final PaymentWebhookEventRepository webhookEvents;
    private final OrderRepository orders;
    private final OfflineBillRepository bills;
    private final UserRepository users;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final OfflineBillPaymentFulfillmentService offlineBillFulfillment;
    private final OfflineBillInvoiceService offlineBillInvoices;
    private final LoyaltyService loyaltyService;

    public RazorpayPaymentService(RazorpayConfig razorpay, PaymentTransactionRepository transactions,
                                  PaymentWebhookEventRepository webhookEvents,
                                  OrderRepository orders, OfflineBillRepository bills,
                                  UserRepository users, ObjectMapper objectMapper, EmailService emailService,
                                  OfflineBillPaymentFulfillmentService offlineBillFulfillment,
                                  OfflineBillInvoiceService offlineBillInvoices, LoyaltyService loyaltyService) {
        this.razorpay = razorpay; this.transactions = transactions; this.orders = orders;
        this.webhookEvents = webhookEvents; this.bills = bills; this.users = users;
        this.objectMapper = objectMapper; this.emailService = emailService;
        this.offlineBillFulfillment = offlineBillFulfillment;
        this.offlineBillInvoices = offlineBillInvoices;
        this.loyaltyService = loyaltyService;
    }

    @Transactional
    public RazorpayOrderResponse createOnlineOrder(Long orderId, String email) {
        User customer = customer(email);
        Order order = ownedOrder(orderId, customer);
        if (order.getOrderStatus() == OrderStatus.CANCELLED) throw new IllegalStateException("Cancelled orders cannot be paid");
        if (order.getPaymentStatus() == PaymentStatus.PAID) throw new IllegalStateException("This order is already paid");
        PaymentTransaction reusable = transactions.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(payment -> payment.getPaymentMethod() == PaymentMethod.RAZORPAY)
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.CREATED || payment.getPaymentStatus() == PaymentStatus.AUTHORIZED)
                .findFirst().orElse(null);
        if (reusable != null) return onlineResponse(order, customer, reusable);
        PaymentTransaction payment = createGatewayTransaction(PaymentContext.ONLINE_ORDER, order.getGrandTotal(),
                order.getId(), null, customer.getId(), null, customer.getName(), customer.getMobile(),
                "ShivHub Order #" + order.getOrderNumber());
        order.setPaymentMethod(PaymentMethod.RAZORPAY);
        order.setPaymentStatus(PaymentStatus.CREATED);
        orders.save(order);
        return onlineResponse(order, customer, payment);
    }

    @Transactional
    public RazorpayVerificationResponse verifyOnline(RazorpayVerificationRequest request, String email) {
        User customer = customer(email);
        Order order = ownedOrder(request.getInternalOrderId(), customer);
        PaymentTransaction payment = gatewayPayment(request.getRazorpayOrderId(), PaymentContext.ONLINE_ORDER, order.getId(), null);
        verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
        markPaid(payment, request.getRazorpayPaymentId(), request.getRazorpaySignature());
        settle(payment);
        return new RazorpayVerificationResponse(true, PaymentStatus.PAID, order.getOrderStatus(), order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse onlineStatus(Long orderId, String email) {
        User customer = customer(email); Order order = ownedOrder(orderId, customer);
        List<PaymentTransactionResponse> history = transactions.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::view).toList();
        BigDecimal paid = history.stream().filter(item -> item.getPaymentStatus() == PaymentStatus.PAID)
                .map(PaymentTransactionResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaymentSummaryResponse(order.getGrandTotal(), paid, positive(order.getGrandTotal().subtract(paid)), BigDecimal.ZERO,
                order.getPaymentStatus(), history);
    }

    @Transactional
    public RazorpayOrderResponse createOfflineOrder(Long billId, String email) {
        User seller = seller(email); OfflineBill bill = ownedBill(billId, seller);
        BigDecimal due = offlineDue(bill);
        if (due.signum() <= 0) throw new IllegalStateException("This bill is already fully paid");
        PaymentTransaction reusable = transactions.findByOfflineBillIdOrderByCreatedAtDesc(billId).stream()
                .filter(payment -> payment.getPaymentMethod() == PaymentMethod.RAZORPAY)
                .filter(payment -> payment.getRazorpayOrderId() != null)
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.CREATED || payment.getPaymentStatus() == PaymentStatus.AUTHORIZED)
                .findFirst().orElse(null);
        if (reusable != null) return offlineResponse(bill, reusable);
        boolean linkAlreadyPending = transactions.findByOfflineBillIdOrderByCreatedAtDesc(billId).stream()
                .anyMatch(payment -> payment.getPaymentMethod() == PaymentMethod.RAZORPAY
                        && payment.getRazorpayPaymentLinkId() != null
                        && (payment.getPaymentStatus() == PaymentStatus.CREATED || payment.getPaymentStatus() == PaymentStatus.AUTHORIZED));
        if (linkAlreadyPending) throw new IllegalStateException("A Razorpay payment link is already pending for this bill");
        PaymentTransaction payment = createGatewayTransaction(PaymentContext.OFFLINE_BILL, due, null, bill.getId(), bill.getCustomerId(), seller.getId(),
                bill.getCustomerName(), bill.getCustomerMobile(), "ShivHub Bill #" + bill.getBillNumber());
        bill.setPaymentStatus(PaymentStatus.CREATED);
        bills.save(bill);
        return offlineResponse(bill, payment);
    }

    /** Creates a seller-owned hosted Razorpay Payment Link for the live due amount. */
    @Transactional
    public RazorpayPaymentLinkResponse createOfflinePaymentLink(Long billId, String email) {
        User seller = seller(email); OfflineBill bill = ownedBill(billId, seller);
        BigDecimal due = offlineDue(bill);
        if (due.signum() <= 0) throw new IllegalStateException("This bill is already fully paid");
        PaymentTransaction reusable = transactions.findByOfflineBillIdOrderByCreatedAtDesc(billId).stream()
                .filter(payment -> payment.getPaymentMethod() == PaymentMethod.RAZORPAY)
                .filter(payment -> payment.getRazorpayPaymentLinkId() != null)
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.CREATED || payment.getPaymentStatus() == PaymentStatus.AUTHORIZED)
                .findFirst().orElse(null);
        if (reusable != null) return paymentLinkResponse(bill, reusable);
        boolean checkoutAlreadyPending = transactions.findByOfflineBillIdOrderByCreatedAtDesc(billId).stream()
                .anyMatch(payment -> payment.getPaymentMethod() == PaymentMethod.RAZORPAY
                        && payment.getRazorpayOrderId() != null
                        && (payment.getPaymentStatus() == PaymentStatus.CREATED || payment.getPaymentStatus() == PaymentStatus.AUTHORIZED));
        if (checkoutAlreadyPending) throw new IllegalStateException("A Razorpay checkout collection is already pending for this bill");

        PaymentTransaction payment = newPayment(PaymentContext.OFFLINE_BILL, PaymentMethod.RAZORPAY, due,
                null, bill.getId(), bill.getCustomerId(), seller.getId(), bill.getCustomerName(), bill.getCustomerMobile());
        try {
            JSONObject request = new JSONObject();
            request.put("amount", paise(due)); request.put("currency", "INR");
            request.put("accept_partial", false); request.put("reference_id", payment.getPaymentReference());
            request.put("description", "ShivHub Bill #" + bill.getBillNumber());
            JSONObject customer = new JSONObject();
            if (bill.getCustomerName() != null && !bill.getCustomerName().isBlank()) customer.put("name", bill.getCustomerName());
            if (bill.getCustomerMobile() != null && !bill.getCustomerMobile().isBlank()) customer.put("contact", bill.getCustomerMobile());
            if (bill.getCustomerEmail() != null && !bill.getCustomerEmail().isBlank()) customer.put("email", bill.getCustomerEmail());
            if (!customer.isEmpty()) request.put("customer", customer);
            JSONObject notify = new JSONObject();
            notify.put("sms", bill.getCustomerMobile() != null && !bill.getCustomerMobile().isBlank());
            notify.put("email", bill.getCustomerEmail() != null && !bill.getCustomerEmail().isBlank());
            request.put("notify", notify);
            JSONObject notes = new JSONObject(); notes.put("shivhub_reference", payment.getPaymentReference()); notes.put("offline_bill_id", billId);
            request.put("notes", notes);
            com.razorpay.PaymentLink link = razorpay.client().paymentLink.create(request);
            payment.setRazorpayPaymentLinkId(link.get("id")); payment.setRazorpayPaymentLinkUrl(link.get("short_url"));
            payment.setPaymentStatus(PaymentStatus.CREATED); payment.setNotes("ShivHub Bill #" + bill.getBillNumber());
            payment = transactions.save(payment);
            bill.setPaymentStatus(PaymentStatus.CREATED); bills.save(bill);
            return paymentLinkResponse(bill, payment);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Razorpay payment link");
        }
    }

    @Transactional
    public PaymentSummaryResponse verifyOffline(Long billId, RazorpayVerificationRequest request, String email) {
        User seller = seller(email); OfflineBill bill = ownedBill(billId, seller);
        PaymentTransaction payment = gatewayPayment(request.getRazorpayOrderId(), PaymentContext.OFFLINE_BILL, null, billId);
        verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
        markPaid(payment, request.getRazorpayPaymentId(), request.getRazorpaySignature());
        settle(payment);
        return offlineSummary(bill);
    }

    @Transactional
    public PaymentSummaryResponse recordManualOfflinePayment(Long billId, ManualPaymentRequest request, String email) {
        User seller = seller(email); OfflineBill bill = ownedBill(billId, seller);
        if (request.getPaymentMethod() == PaymentMethod.RAZORPAY || request.getPaymentMethod() == PaymentMethod.MIXED)
            throw new IllegalArgumentException("Choose Cash, UPI, Card, Bank Transfer or Cheque for a manual payment");
        BigDecimal due = offlineDue(bill); BigDecimal amount = money(request.getAmount());
        if (amount.signum() <= 0) throw new IllegalArgumentException("Payment amount must be positive");
        if (request.getPaymentMethod() != PaymentMethod.CASH && amount.compareTo(due) > 0)
            throw new IllegalArgumentException("Manual payment cannot exceed the remaining due");
        BigDecimal applied = amount.min(due);
        PaymentTransaction payment = newPayment(PaymentContext.OFFLINE_BILL, request.getPaymentMethod(), applied,
                null, billId, bill.getCustomerId(), seller.getId(), bill.getCustomerName(), bill.getCustomerMobile());
        payment.setPaymentStatus(PaymentStatus.PAID); payment.setPaidAt(LocalDateTime.now());
        payment.setNotes(request.getNotes()); payment.setTransactionReference(request.getTransactionReference());
        transactions.save(payment);
        applyOfflinePayment(bill, applied, request.getPaymentMethod());
        return offlineSummary(bill, amount.subtract(applied));
    }

    /** Records the existing POS form's first Cash/Manual payment without changing its calculation flow. */
    @Transactional
    public void recordInitialOfflinePayment(OfflineBill bill, User seller, BigDecimal receivedAmount,
                                            PaymentMethod method, String transactionReference, String notes) {
        BigDecimal amount = money(receivedAmount);
        if (amount.signum() <= 0) { bill.setPaymentStatus(PaymentStatus.PENDING); bills.save(bill); return; }
        BigDecimal applied = amount.min(money(bill.getGrandTotal()));
        PaymentTransaction payment = newPayment(PaymentContext.OFFLINE_BILL, method, applied, null, bill.getId(),
                bill.getCustomerId(), seller.getId(), bill.getCustomerName(), bill.getCustomerMobile());
        payment.setPaymentStatus(PaymentStatus.PAID); payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionReference(transactionReference); payment.setNotes(notes);
        transactions.save(payment);
        bill.setPaymentStatus(amount.compareTo(money(bill.getGrandTotal())) >= 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
        bills.save(bill);
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse offlineHistory(Long billId, String email) { return offlineSummary(ownedBill(billId, seller(email))); }

    /** Webhooks are already HMAC-authenticated by the controller and are idempotent here. */
    @Transactional
    public void processWebhook(String event, String rawBody, String gatewayEventId) {
        if (gatewayEventId != null && !gatewayEventId.isBlank()
                && webhookEvents.existsByGatewayEventId(gatewayEventId)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody); JsonNode payload = root.path("payload");
            JsonNode paymentNode = payload.path("payment").path("entity"); JsonNode orderNode = payload.path("order").path("entity");
            JsonNode linkNode = payload.path("payment_link").path("entity");
            String paymentId = text(paymentNode, "id"); String gatewayOrderId = text(paymentNode, "order_id");
            if (gatewayOrderId == null) gatewayOrderId = text(orderNode, "id");
            String paymentLinkId = text(linkNode, "id");
            PaymentTransaction transaction = gatewayOrderId != null
                    ? transactions.findByRazorpayOrderId(gatewayOrderId).orElse(null)
                    : paymentLinkId == null ? null : transactions.findByRazorpayPaymentLinkId(paymentLinkId).orElse(null);
            String result = "IGNORED";

            if (gatewayOrderId == null && paymentLinkId == null) {
                result = "MISSING_PAYMENT_REFERENCE";
            } else if (transaction == null) {
                result = "UNMATCHED_ORDER";
            } else {
                long amount = paymentNode.path("amount").asLong(0); String currency = text(paymentNode, "currency");
                if (amount > 0 && (amount != paise(transaction.getAmount())
                        || (currency != null && !"INR".equalsIgnoreCase(currency)))) {
                    result = "AMOUNT_OR_CURRENCY_MISMATCH";
                } else if ("payment.failed".equals(event)) {
                    if (transaction.getPaymentStatus() != PaymentStatus.PAID) {
                        transaction.setPaymentStatus(PaymentStatus.FAILED);
                        transaction.setFailureCode(text(paymentNode.path("error"), "code"));
                        transaction.setFailureReason(text(paymentNode.path("error"), "description"));
                        transactions.save(transaction);
                        if (transaction.getPaymentContext() == PaymentContext.ONLINE_ORDER) {
                            Order order = orders.findById(transaction.getOrderId()).orElse(null);
                            if (order != null && order.getPaymentStatus() != PaymentStatus.PAID) {
                                order.setPaymentStatus(PaymentStatus.FAILED);
                                orders.save(order);
                            }
                        }
                    }
                    result = "FAILED_RECORDED";
                } else if ("payment.authorized".equals(event)) {
                    if (transaction.getPaymentStatus() == PaymentStatus.CREATED) {
                        transaction.setPaymentStatus(PaymentStatus.AUTHORIZED);
                        transactions.save(transaction);
                    }
                    result = "AUTHORIZED_RECORDED";
                } else if ("payment.captured".equals(event) || "order.paid".equals(event) || "payment_link.paid".equals(event)) {
                    markPaid(transaction, paymentId, null);
                    settle(transaction);
                    result = "PAID_SETTLED";
                } else if ("refund.created".equals(event) || "refund.processed".equals(event)) {
                    transaction.setRazorpayRefundId(text(payload.path("refund").path("entity"), "id"));
                    transaction.setPaymentStatus("refund.processed".equals(event)
                            ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
                    transactions.save(transaction);
                    if (transaction.getPaymentContext() == PaymentContext.ONLINE_ORDER) {
                        orders.findById(transaction.getOrderId()).ifPresent(order -> loyaltyService.reverseForOnlineOrder(order, "Payment refunded"));
                    } else if (transaction.getPaymentContext() == PaymentContext.OFFLINE_BILL) {
                        bills.findById(transaction.getOfflineBillId()).ifPresent(bill -> loyaltyService.reverseForOfflineBill(bill, "Payment refunded"));
                    }
                    result = "REFUND_RECORDED";
                } else {
                    result = "UNHANDLED_EVENT";
                }
            }
            recordWebhookEvent(gatewayEventId, event, gatewayOrderId, transaction, result);
        } catch (Exception exception) { throw new IllegalStateException("Unable to process Razorpay webhook"); }
    }

    public boolean validWebhookSignature(String rawBody, String signature) {
        if (signature == null || signature.isBlank() || razorpay.getWebhookSecret() == null || razorpay.getWebhookSecret().isBlank()) return false;
        return constantTimeEquals(hmac(rawBody, razorpay.getWebhookSecret()), signature);
    }

    private PaymentTransaction createGatewayTransaction(PaymentContext context, BigDecimal amount, Long orderId, Long billId,
                                                         Long customerId, Long sellerId, String name, String mobile, String description) {
        BigDecimal safeAmount = money(amount); if (safeAmount.signum() <= 0) throw new IllegalArgumentException("Payment amount must be positive");
        PaymentTransaction payment = newPayment(context, PaymentMethod.RAZORPAY, safeAmount, orderId, billId, customerId, sellerId, name, mobile);
        try {
            JSONObject request = new JSONObject(); request.put("amount", paise(safeAmount)); request.put("currency", "INR"); request.put("receipt", payment.getPaymentReference());
            JSONObject notes = new JSONObject(); notes.put("shivhub_reference", payment.getPaymentReference()); notes.put("context", context.name()); request.put("notes", notes);
            com.razorpay.Order gatewayOrder = razorpay.client().orders.create(request);
            payment.setRazorpayOrderId(gatewayOrder.get("id")); payment.setPaymentStatus(PaymentStatus.CREATED); payment.setNotes(description);
            return transactions.save(payment);
        } catch (Exception exception) { throw new IllegalStateException("Unable to create Razorpay payment order"); }
    }

    private PaymentTransaction gatewayPayment(String razorpayOrderId, PaymentContext context, Long orderId, Long billId) {
        PaymentTransaction payment = transactions.findByRazorpayOrderId(razorpayOrderId).orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        if (payment.getPaymentContext() != context || (orderId != null && !orderId.equals(payment.getOrderId())) || (billId != null && !billId.equals(payment.getOfflineBillId())))
            throw new SecurityException("Payment does not belong to this record");
        return payment;
    }
    private void verifySignature(String orderId, String paymentId, String signature) { if (!constantTimeEquals(hmac(orderId + "|" + paymentId, razorpay.getKeySecret()), signature)) throw new SecurityException("Invalid Razorpay payment signature"); }
    private void markPaid(PaymentTransaction payment, String paymentId, String signature) {
        if (payment.getPaymentStatus() == PaymentStatus.PAID) { if (paymentId != null && payment.getRazorpayPaymentId() != null && !paymentId.equals(payment.getRazorpayPaymentId())) throw new IllegalStateException("Payment was already verified"); return; }
        if (paymentId != null) { PaymentTransaction existing = transactions.findByRazorpayPaymentId(paymentId).orElse(null); if (existing != null && !existing.getId().equals(payment.getId())) throw new IllegalStateException("Gateway payment has already been recorded"); payment.setRazorpayPaymentId(paymentId); }
        if (signature != null) payment.setRazorpaySignature(signature); payment.setPaymentStatus(PaymentStatus.PAID); payment.setPaidAt(LocalDateTime.now()); transactions.save(payment);
    }
    private void settle(PaymentTransaction payment) {
        if (payment.getSettledAt() != null) return;
        if (payment.getPaymentContext() == PaymentContext.ONLINE_ORDER) confirmOrder(orders.findById(payment.getOrderId()).orElseThrow());
        if (payment.getPaymentContext() == PaymentContext.OFFLINE_BILL) applyOfflinePayment(bills.findById(payment.getOfflineBillId()).orElseThrow(), payment.getAmount(), PaymentMethod.RAZORPAY);
        payment.setSettledAt(LocalDateTime.now()); transactions.save(payment);
    }
    private void confirmOrder(Order order) { if (order.getPaymentStatus() == PaymentStatus.PAID) { loyaltyService.awardForOnlineOrder(order); return; } order.setPaymentStatus(PaymentStatus.PAID); order.setPaymentMethod(PaymentMethod.RAZORPAY); if (order.getOrderStatus() == OrderStatus.PENDING) order.setOrderStatus(OrderStatus.CONFIRMED); orders.save(order); loyaltyService.awardForOnlineOrder(order); try { User customer = users.findById(order.getCustomerId()).orElse(null); if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) emailService.sendOrderConfirmationEmail(customer.getEmail(), customer.getName(), order.getOrderNumber(), order.getGrandTotal().setScale(2).toPlainString(), null); } catch (Exception ignored) { /* Payment is already durable even if notification delivery fails. */ } }
    private void applyOfflinePayment(OfflineBill bill, BigDecimal amount, PaymentMethod method) {
        if (bill.getPaymentStatus() == PaymentStatus.PAID) return;
        BigDecimal paid = money(bill.getPaymentAmount()).add(money(amount));
        bill.setPaymentAmount(paid);
        bill.setPaymentMethod(bill.getPaymentMethod() == null || bill.getPaymentMethod() == method
                ? method : PaymentMethod.MIXED);
        boolean fullyPaid = paid.compareTo(money(bill.getGrandTotal())) >= 0;
        bill.setPaymentStatus(fullyPaid ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
        bills.save(bill);
        if (fullyPaid) {
            offlineBillFulfillment.finalizeIfPending(bill);
            loyaltyService.awardForOfflineBill(bill);
            sendOfflinePaymentConfirmation(bill);
        }
    }
    private RazorpayOrderResponse onlineResponse(Order order, User customer, PaymentTransaction payment) { return new RazorpayOrderResponse(order.getId(), null, payment.getRazorpayOrderId(), paise(payment.getAmount()), "INR", razorpay.getKeyId(), customer.getName(), customer.getEmail(), customer.getMobile(), "ShivHub Order #" + order.getOrderNumber()); }
    private RazorpayOrderResponse offlineResponse(OfflineBill bill, PaymentTransaction payment) { return new RazorpayOrderResponse(null, bill.getId(), payment.getRazorpayOrderId(), paise(payment.getAmount()), "INR", razorpay.getKeyId(), bill.getCustomerName(), bill.getCustomerEmail(), bill.getCustomerMobile(), "ShivHub Bill #" + bill.getBillNumber()); }
    private RazorpayPaymentLinkResponse paymentLinkResponse(OfflineBill bill, PaymentTransaction payment) { return new RazorpayPaymentLinkResponse(bill.getId(), payment.getRazorpayPaymentLinkId(), payment.getRazorpayPaymentLinkUrl(), paise(payment.getAmount()), "INR", "ShivHub Bill #" + bill.getBillNumber()); }
    private PaymentSummaryResponse offlineSummary(OfflineBill bill) { return offlineSummary(bill, BigDecimal.ZERO); }
    private PaymentSummaryResponse offlineSummary(OfflineBill bill, BigDecimal change) { List<PaymentTransactionResponse> history = transactions.findByOfflineBillIdOrderByCreatedAtDesc(bill.getId()).stream().map(this::view).toList(); return new PaymentSummaryResponse(money(bill.getGrandTotal()), money(bill.getPaymentAmount()), offlineDue(bill), positive(change), bill.getPaymentStatus() == null ? PaymentStatus.PENDING : bill.getPaymentStatus(), history); }
    private PaymentTransaction newPayment(PaymentContext context, PaymentMethod method, BigDecimal amount, Long orderId, Long billId, Long customerId, Long sellerId, String name, String mobile) { PaymentTransaction payment = new PaymentTransaction(); payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT)); payment.setPaymentContext(context); payment.setPaymentMethod(method); payment.setAmount(money(amount)); payment.setCurrency("INR"); payment.setOrderId(orderId); payment.setOfflineBillId(billId); payment.setCustomerId(customerId); payment.setSellerId(sellerId); payment.setPayerName(name); payment.setPayerMobile(mobile); return payment; }
    private PaymentTransactionResponse view(PaymentTransaction item) { return new PaymentTransactionResponse(item.getId(), item.getPaymentReference(), item.getRazorpayOrderId(), item.getRazorpayPaymentId(), item.getPaymentContext(), item.getPaymentMethod(), item.getPaymentStatus(), item.getAmount(), item.getCurrency(), item.getTransactionReference(), item.getNotes(), item.getPaidAt(), item.getCreatedAt()); }
    private void recordWebhookEvent(String gatewayEventId, String event, String gatewayOrderId,
                                    PaymentTransaction transaction, String result) {
        if (gatewayEventId == null || gatewayEventId.isBlank()) return;
        PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent();
        webhookEvent.setGatewayEventId(gatewayEventId);
        webhookEvent.setEventType(event == null ? "unknown" : event);
        webhookEvent.setRazorpayOrderId(gatewayOrderId);
        webhookEvent.setPaymentTransactionId(transaction == null ? null : transaction.getId());
        webhookEvent.setProcessingResult(result);
        webhookEvent.setProcessedAt(LocalDateTime.now());
        webhookEvents.save(webhookEvent);
    }
    private void sendOfflinePaymentConfirmation(OfflineBill bill) {
        if (bill.getCustomerEmail() == null || bill.getCustomerEmail().isBlank()) return;
        try {
            byte[] pdf = offlineBillInvoices.generateInvoicePdf(bill);
            emailService.sendOfflineBillEmail(bill.getCustomerEmail(),
                    bill.getCustomerName() == null ? "Customer" : bill.getCustomerName(),
                    bill.getBillNumber(), money(bill.getGrandTotal()).toPlainString(),
                    bill.getPaymentMethod() == null ? PaymentMethod.RAZORPAY.name() : bill.getPaymentMethod().name(), pdf);
        } catch (Exception ignored) {
            // A confirmed payment must never be rolled back due to notification delivery.
        }
    }
    private User customer(String email) { User user = users.findByEmail(email).orElseThrow(() -> new SecurityException("Customer not found")); if (user.getRole() != Role.CUSTOMER) throw new SecurityException("Only customers can pay online orders"); return user; }
    private User seller(String email) { User user = users.findByEmail(email).orElseThrow(() -> new SecurityException("Seller not found")); if (user.getRole() != Role.SELLER) throw new SecurityException("Only sellers can collect offline payments"); return user; }
    private Order ownedOrder(Long id, User customer) { Order order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found")); if (!customer.getId().equals(order.getCustomerId())) throw new SecurityException("Order does not belong to this customer"); return order; }
    private OfflineBill ownedBill(Long id, User seller) { OfflineBill bill = bills.findById(id).orElseThrow(() -> new IllegalArgumentException("Offline bill not found")); if (!seller.getId().equals(bill.getSellerId())) throw new SecurityException("Bill does not belong to this seller"); return bill; }
    private BigDecimal offlineDue(OfflineBill bill) { return positive(money(bill.getGrandTotal()).subtract(money(bill.getPaymentAmount()))); }
    private BigDecimal money(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal positive(BigDecimal amount) { return amount.signum() < 0 ? BigDecimal.ZERO : amount; }
    private long paise(BigDecimal amount) { return money(amount).movePointRight(2).longValueExact(); }
    private String hmac(String payload, String secret) { try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException("Unable to verify payment signature"); } }
    private boolean constantTimeEquals(String left, String right) { return left != null && right != null && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)); }
    private String text(JsonNode node, String key) { JsonNode value = node.path(key); return value.isTextual() && !value.asText().isBlank() ? value.asText() : null; }
}
