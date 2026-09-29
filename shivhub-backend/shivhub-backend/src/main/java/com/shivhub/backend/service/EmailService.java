package com.shivhub.backend.service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.shivhub.backend.entity.MarketingCampaign;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


/*
 * =========================================================
 * EmailService
 * =========================================================
 *
 * Central email service for ShivHub.
 *
 * Handles:
 *
 * 1. Product Approved
 * 2. Product Rejected
 * 3. Seller Approved
 * 4. Seller Rejected
 * 5. Login OTP
 * 6. Marketing Emails
 * 7. Order Confirmation
 * 8. Invoice PDF Email
 * 9. Order Status Updates
 *
 * =========================================================
 */

@Service
public class EmailService {
    /** Finance callers record delivery success/failure after the bill transaction commits. */
    public void sendFinanceNotification(String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) throw new IllegalArgumentException("Customer email is required");
        send(recipient, subject, body);
    }


    /*
     * =========================================================
     * JAVA MAIL SENDER
     * =========================================================
     */

    private final JavaMailSender mailSender;
    private final AdminIntegrationCredentialService integrationCredentials;
    private final WhatsAppNotificationService whatsappNotifications;
    /** Public API base is used only to turn an uploaded /uploads/... banner into an email-safe absolute URL. */
    private final String publicApiUrl;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public EmailService(
            JavaMailSender mailSender,
            AdminIntegrationCredentialService integrationCredentials,
            WhatsAppNotificationService whatsappNotifications,
            @Value("${shivhub.public-api-url:http://localhost:8081}") String publicApiUrl) {

        this.mailSender = mailSender;
        this.integrationCredentials = integrationCredentials;
        this.whatsappNotifications = whatsappNotifications;
        this.publicApiUrl = publicApiUrl == null ? "" : publicApiUrl.replaceAll("/+$", "");
    }

    private JavaMailSender activeMailSender() {
        return integrationCredentials.mailSender(mailSender);
    }


    /*
     * =========================================================
     * PRODUCT APPROVED EMAIL
     * =========================================================
     */

    public void sendProductApprovedEmail(
            String sellerEmail,
            String productName,
            String reviewMessage) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(sellerEmail);

        message.setSubject(
                "ShivHub - Product Approved"
        );

        message.setText(

                "Hello,\n\n"

                + "Good news! Your product has been "
                + "approved by ShivHub Admin.\n\n"

                + "Product: "
                + productName
                + "\n\n"

                + "Status: APPROVED\n\n"

                + "Admin Review:\n"
                + reviewMessage
                + "\n\n"

                + "Your product can now be visible "
                + "to ShivHub customers.\n\n"

                + "Thank you for selling on ShivHub.\n\n"

                + "Regards,\n"
                + "ShivHub Team"
        );

        activeMailSender().send(message);
        sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_PRODUCT_APPROVED, null, productName,
                java.util.List.of(safeText(productName), safeText(reviewMessage)));
    }


    /*
     * =========================================================
     * LOGIN OTP EMAIL
     * =========================================================
     */

    public void sendLoginOtpEmail(
            String recipientEmail,
            String otp) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(recipientEmail);

        message.setSubject(
                "ShivHub login verification code"
        );

        message.setText(

                "Your ShivHub verification code is: "
                + otp

                + "\n\n"

                + "It expires in 10 minutes. "
                + "Do not share this code with anyone."
        );

        activeMailSender().send(message);
        sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.OTP_LOGIN, "Customer", "login-otp",
                java.util.List.of(otp));
    }

    public void sendSellerRegistrationOtpEmail(
            String recipientEmail,
            String sellerName,
            String businessName,
            String otp) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(recipientEmail);

        message.setSubject(
                "ShivHub seller email verification code"
        );

        message.setText(
                "Hello "
                + (sellerName == null ? "Seller" : sellerName)
                + ",\n\n"
                + "Use this OTP to verify your seller registration email:\n\n"
                + otp
                + "\n\n"
                + "Shop: "
                + (businessName == null ? "-" : businessName)
                + "\n\n"
                + "This code expires in 10 minutes. After verification, "
                + "your seller application will go to ShivHub Admin for approval.\n\n"
                + "Regards,\n"
                + "ShivHub Team"
        );

        activeMailSender().send(message);
    }


    /**
     * Staff receives a clear branch/access notification as soon as an owner
     * creates the account. The password is intentionally not sent by email.
     */
    public void sendStaffAssignedEmail(
            String staffEmail,
            String staffName,
            String shopName,
            String accessRole) {

        send(staffEmail,
                "ShivHub - You were added to " + shopName,
                "Hello " + staffName + ",\n\n"
                        + "Your ShivHub staff account is active.\n\n"
                        + "Shop: " + shopName + "\n"
                        + "Access: " + accessRole + "\n\n"
                        + "Use the password shared securely by your owner to sign in. "
                        + "Do not share your login details.\n\nRegards,\nShivHub Team");
        sendUserWhatsApp(staffEmail, WhatsAppNotificationEvent.STAFF_ACCOUNT_ASSIGNED, staffName, shopName,
                java.util.List.of(safeText(staffName), safeText(shopName), safeText(accessRole)));
    }

    /** Distributor workflow email for submission, approval, rejection and admin changes. */
    public void sendDistributorWorkflowEmail(String email, String subject, String message) {
        if (email == null || email.isBlank()) return;
        send(email, "ShivHub - " + subject, message + "\n\nRegards,\nShivHub Team");
        sendUserWhatsApp(email, WhatsAppNotificationEvent.DISTRIBUTOR_WORKFLOW_UPDATED, null, subject,
                java.util.List.of(safeText(subject), safeText(message)));
    }

    /** Consolidated seller-only due report. SMTP settings resolve securely from Admin or the environment fallback. */
    public void sendSellerDailyPaymentReminder(String email, String shopName, String report) {
        if (email == null || email.isBlank()) return;
        send(email, "ShivHub - Daily payment reminder", "Hello " + shopName + ",\n\n" + report
                + "\n\nOpen your ShivHub seller dashboard to record or follow up on payments.\n\nRegards,\nShivHub Team");
        sendUserWhatsApp(email, WhatsAppNotificationEvent.SELLER_PAYMENT_REMINDER, null, shopName,
                java.util.List.of(safeText(shopName), safeText(report)));
    }

    /** Plain, trustworthy seller-to-customer email template for offers and greetings. */
    public void sendSellerCustomerCampaignEmail(String recipient, String shopName, String subject, String campaignMessage) {
        sendSellerCustomerCampaignEmail(recipient, shopName, subject, campaignMessage, null);
    }

    /** Subscription notices deliberately reuse the standard mail transport and never affect access on mail failure. */
    public void sendSubscriptionNotice(String recipientEmail, String sellerName, String subject, String message) {
        if (recipientEmail == null || recipientEmail.isBlank()) return;
        send(recipientEmail, subject, "Hello " + (sellerName == null || sellerName.isBlank() ? "Seller" : sellerName)
                + ",\n\n" + message + "\n\nRegards,\nShivHub Team");
        sendUserWhatsApp(recipientEmail, WhatsAppNotificationEvent.SELLER_SUBSCRIPTION_UPDATED, sellerName, subject,
                java.util.List.of(safeText(subject), safeText(message)));
    }

    /** Seller offers use the same safe HTML template as ShivHub campaigns, optionally with a banner. */
    public void sendSellerCustomerCampaignEmail(String recipient, String shopName, String subject, String campaignMessage, String bannerUrl) {
        try {
            sendHtml(recipient, safeText(shopName) + " - " + safeText(subject),
                    promotionalHtml("Hello", campaignMessage, null, null, bannerUrl,
                            "Thank you for visiting " + safeText(shopName) + ".", safeText(shopName)));
            sendUserWhatsApp(recipient, WhatsAppNotificationEvent.OFFER_NOTIFICATION, null, subject,
                    java.util.List.of(safeText(shopName), safeText(subject), safeText(campaignMessage)));
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not prepare seller offer email", exception);
        }
    }

    /** Customer collection receipt for the append-only credit-sale ledger. */
    public void sendCustomerPaymentReceivedEmail(String email, String customerName, String shopName,
            String invoiceNumber, BigDecimal amount, BigDecimal remaining, String reference) {
        if (email == null || email.isBlank()) return;
        send(email, shopName + " - Payment received",
                "Hello " + (customerName == null ? "Customer" : customerName) + ",\n\n"
                + "We received ₹" + amount + " against " + (invoiceNumber == null ? "your credit purchase" : "invoice " + invoiceNumber) + ".\n"
                + "Reference: " + reference + "\nRemaining balance: ₹" + remaining + "\n\n"
                + "Thank you,\n" + shopName + "\nPowered by ShivHub");
        sendCustomerWhatsApp(email, WhatsAppNotificationEvent.PAYMENT_SUCCESS, customerName, invoiceNumber,
                java.util.List.of(safeText(customerName), safeText(invoiceNumber),
                        amount == null ? "" : amount.toPlainString(), remaining == null ? "" : remaining.toPlainString(),
                        safeText(reference)));
    }

    /** Customer-safe receipt after a seller/admin records an online order payment status. */
    public void sendOnlineOrderPaymentEmail(String email, String customerName, String orderNumber,
            String paymentStatus, BigDecimal orderAmount) {
        if (email == null || email.isBlank()) return;
        String amount = orderAmount == null ? "" : "\nOrder amount: ₹" + orderAmount + "\n";
        String status = paymentStatus == null ? "UPDATED" : paymentStatus.replace('_', ' ');
        send(email, "ShivHub - Payment " + status + " - " + orderNumber,
                "Hello " + (customerName == null || customerName.isBlank() ? "Customer" : customerName) + ",\n\n"
                        + "Your payment status for order " + orderNumber + " is now " + status + ".\n"
                        + amount
                        + "\nYou can view the latest order details in My Orders.\n\nRegards,\nShivHub Team");
        WhatsAppNotificationEvent event = paymentStatus != null && paymentStatus.toUpperCase().contains("REFUND")
                ? WhatsAppNotificationEvent.REFUND_COMPLETED : WhatsAppNotificationEvent.PAYMENT_SUCCESS;
        sendCustomerWhatsApp(email, event, customerName, orderNumber,
                java.util.List.of(safeText(customerName), safeText(orderNumber), safeText(status),
                        orderAmount == null ? "" : orderAmount.toPlainString()));
    }

    /** Sent when a seller or admin changes the promised delivery date/time for an online order. */
    public void sendOrderDeliveryPromiseEmail(String email, String customerName, String orderNumber,
            LocalDateTime expectedDeliveryAt, String customerMessage) {
        if (email == null || email.isBlank() || expectedDeliveryAt == null) return;
        String expected = expectedDeliveryAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        send(email, "ShivHub - Delivery time update - " + orderNumber,
                "Hello " + (customerName == null || customerName.isBlank() ? "Customer" : customerName) + ",\n\n"
                        + "The expected delivery time for your order " + orderNumber + " has been updated.\n\n"
                        + "Expected delivery: " + expected + "\n"
                        + (customerMessage == null || customerMessage.isBlank() ? "" : "\n" + customerMessage.trim() + "\n")
                        + "\nYou can view the latest delivery promise in My Orders.\n\nRegards,\nShivHub Team");
        sendCustomerWhatsApp(email, WhatsAppNotificationEvent.DELIVERY_TIME_UPDATED, customerName, orderNumber,
                java.util.List.of(safeText(customerName), safeText(orderNumber), expected, safeText(customerMessage)));
    }

    /** Transactional receipt for a points balance change after a successful sale. */
    public void sendLoyaltyPointsEmail(String recipientEmail, String customerName, String reference,
            long earnedPoints, long redeemedPoints, long availablePoints, BigDecimal redemptionDiscount) {
        if (recipientEmail == null || recipientEmail.isBlank()) return;

        StringBuilder body = new StringBuilder()
                .append("Hello ").append(customerName == null || customerName.isBlank() ? "Customer" : customerName).append(",\n\n")
                .append("Your ShivHub Points balance was updated for ").append(reference == null || reference.isBlank() ? "your purchase" : reference).append(".\n\n");
        if (earnedPoints > 0) body.append("Points earned: ").append(earnedPoints).append("\n");
        if (redeemedPoints > 0) {
            body.append("Points redeemed: ").append(redeemedPoints).append("\n");
            body.append("Reward discount: Rs. ").append(redemptionDiscount == null ? "0.00" : redemptionDiscount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append("\n");
        }
        body.append("Available ShivHub Points: ").append(availablePoints).append("\n")
                .append("Redemption rate: 3 Points = Rs. 1\n\n")
                .append("Thank you for shopping with ShivHub.\n\nRegards,\nShivHub Team");
        send(recipientEmail, "ShivHub Points update - " + (reference == null ? "Purchase" : reference), body.toString());
        sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.LOYALTY_POINTS_UPDATED, customerName, reference,
                java.util.List.of(safeText(customerName), safeText(reference), String.valueOf(earnedPoints),
                        String.valueOf(redeemedPoints), String.valueOf(availablePoints)));
    }


    /*
     * =========================================================
     * PRODUCT REJECTED EMAIL
     * =========================================================
     */

    public void sendProductRejectedEmail(
            String sellerEmail,
            String productName,
            String rejectionReason) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(sellerEmail);

        message.setSubject(
                "ShivHub - Product Requires Changes"
        );

        message.setText(

                "Hello,\n\n"

                + "Your product was reviewed by "
                + "ShivHub Admin.\n\n"

                + "Product: "
                + productName
                + "\n\n"

                + "Status: REJECTED\n\n"

                + "Reason provided by Admin:\n"
                + rejectionReason
                + "\n\n"

                + "Please review the reason, make the "
                + "necessary changes and resubmit your "
                + "product for approval.\n\n"

                + "Thank you,\n\n"

                + "ShivHub Team"
        );

        activeMailSender().send(message);
        sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_PRODUCT_REJECTED, null, productName,
                java.util.List.of(safeText(productName), safeText(rejectionReason)));
    }


    /*
     * =========================================================
     * SELLER APPROVED EMAIL
     * =========================================================
     */

    public void sendSellerApprovedEmail(
            String sellerEmail,
            String sellerName) {

        send(

                sellerEmail,

                "ShivHub - Seller account approved",

                "Hello "
                + sellerName
                + ",\n\n"

                + "Your ShivHub seller account has "
                + "been approved.\n\n"

                + "You can now sign in and start "
                + "adding products.\n\n"

                + "Regards,\n"
                + "ShivHub Team"
        );
        sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_ACCOUNT_APPROVED, sellerName, "Seller account",
                java.util.List.of(safeText(sellerName), "APPROVED"));
    }


    /*
     * =========================================================
     * SELLER REJECTED EMAIL
     * =========================================================
     */

    public void sendSellerRejectedEmail(
            String sellerEmail,
            String sellerName,
            String reason) {

        send(

                sellerEmail,

                "ShivHub - Seller application update",

                "Hello "
                + sellerName
                + ",\n\n"

                + "Your seller application was "
                + "not approved.\n\n"

                + "Reason:\n"
                + reason
                + "\n\n"

                + "You may update the requested "
                + "information and apply again.\n\n"

                + "Regards,\n"
                + "ShivHub Team"
        );
        sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_ACCOUNT_REJECTED, sellerName, "Seller application",
                java.util.List.of(safeText(sellerName), safeText(reason)));
    }


    /*
     * =========================================================
     * MARKETING EMAIL
     * =========================================================
     */

    public void sendMarketingEmail(
            String recipientEmail,
            String recipientName,
            MarketingCampaign campaign) {
        try {
            String subject = campaign.getEmailSubject() == null || campaign.getEmailSubject().isBlank()
                    ? "ShivHub - " + safeText(campaign.getTitle()) : campaign.getEmailSubject();
            String discount = campaign.getDiscountPercent() == null ? null : "Discount: " + campaign.getDiscountPercent() + "%";
            sendHtml(recipientEmail, subject,
                    promotionalHtml("Hello " + safeText(recipientName), campaign.getDescription(), discount,
                            campaign.getCouponCode(), campaign.getBannerUrl(), campaign.getOfferDetails(), "ShivHub Team"));
            sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.OFFER_NOTIFICATION, recipientName, campaign.getTitle(),
                    java.util.List.of(safeText(recipientName), safeText(campaign.getTitle()), safeText(campaign.getCouponCode()), safeText(campaign.getOfferDetails())));
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not prepare campaign email", exception);
        }
    }

    private void sendHtml(String recipient, String subject, String html) throws MessagingException {
        MimeMessage message = activeMailSender().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(html, true);
        activeMailSender().send(message);
        whatsappNotifications.mirrorCustomerEmail(recipient, subject, html);
    }

    /** Email clients require absolute image URLs; uploaded campaign images are served publicly by the API. */
    private String publicImageUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String url = value.trim();
        if (url.startsWith("https://") || url.startsWith("http://")) return url;
        return url.startsWith("/") && !publicApiUrl.isBlank() ? publicApiUrl + url : null;
    }

    private String promotionalHtml(String greeting, String message, String discount, String coupon,
            String bannerUrl, String additionalDetails, String signOff) {
        String image = publicImageUrl(bannerUrl);
        String banner = image == null ? "" : "<img src=\"" + html(image) + "\" alt=\"Offer banner\" style=\"display:block;width:100%;max-width:620px;height:auto;border:0;border-radius:14px;margin:0 auto 22px\" />";
        String discountBlock = discount == null || discount.isBlank() ? "" : "<p style=\"margin:16px 0 0;font-weight:700;color:#174ea6\">" + html(discount) + "</p>";
        String couponBlock = coupon == null || coupon.isBlank() ? "" : "<p style=\"display:inline-block;margin:16px 0 0;padding:10px 14px;border:1px dashed #2563eb;border-radius:8px;font-weight:800;color:#1d4ed8;background:#eff6ff\">Coupon: " + html(coupon) + "</p>";
        String details = additionalDetails == null || additionalDetails.isBlank() ? "" : "<p style=\"margin:18px 0 0;color:#4b5563\">" + htmlWithBreaks(additionalDetails) + "</p>";
        return "<!doctype html><html><body style=\"margin:0;padding:24px;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#17233b\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"center\">"
                + "<div style=\"max-width:620px;background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 8px 24px rgba(15,23,42,.08)\">"
                + banner + "<p style=\"margin:0 0 14px;font-size:16px\">" + html(greeting) + ",</p>"
                + "<p style=\"margin:0;line-height:1.65;color:#334155\">" + htmlWithBreaks(message) + "</p>"
                + discountBlock + couponBlock + details
                + "<p style=\"margin:26px 0 0;padding-top:18px;border-top:1px solid #e5e7eb;color:#64748b;line-height:1.55\">Regards,<br/><strong style=\"color:#17233b\">" + html(signOff) + "</strong></p>"
                + "</div></td></tr></table></body></html>";
    }

    private String safeText(String value) { return value == null || value.isBlank() ? "ShivHub" : value.trim(); }
    private String htmlWithBreaks(String value) { return html(value == null ? "" : value).replace("\n", "<br/>"); }
    private String html(String value) { return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    /** Notifies both parties after a coupon is committed with an order. */
    public void sendCouponUsedEmail(String customerEmail, String customerName, String orderNumber,
            String couponCode, java.math.BigDecimal discountAmount) {
        String amount = "₹" + discountAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        send(customerEmail, "ShivHub - Coupon applied", "Hello " + customerName + ",\n\nCoupon " + couponCode
                + " was applied to order " + orderNumber + ". You saved " + amount + ".\n\nRegards,\nShivHub Team");
        sendCustomerWhatsApp(customerEmail, WhatsAppNotificationEvent.COUPON_APPLIED, customerName, orderNumber,
                java.util.List.of(safeText(customerName), safeText(orderNumber), safeText(couponCode), amount));
        send("shivhub007@gmail.com", "ShivHub - Coupon used", "Coupon " + couponCode + " was used by "
                + customerName + " on order " + orderNumber + ". Discount given: " + amount + ".");
    }


    /*
     * =========================================================
     * GENERIC SIMPLE EMAIL
     * =========================================================
     */

    private void send(
            String to,
            String subject,
            String body) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(subject);

        message.setText(body);

        sendMessage(message);
    }

    /** Existing email remains authoritative; WhatsApp is a best-effort secondary channel. */
    private void sendMessage(SimpleMailMessage message) {
        activeMailSender().send(message);
        if (message.getTo() == null) return;
        String subject = message.getSubject() == null ? "ShivHub update" : message.getSubject();
        String body = message.getText() == null ? "" : message.getText();
        for (String recipient : message.getTo()) whatsappNotifications.mirrorCustomerEmail(recipient, subject, body);
    }

    /** Backwards-compatible alias for existing customer notification callers. */
    private void sendCustomerWhatsApp(String recipientEmail, WhatsAppNotificationEvent event,
            String customerName, String reference, java.util.List<String> parameters) {
        sendUserWhatsApp(recipientEmail, event, customerName, reference, parameters);
    }

    /** AiSensy receives only explicit event/template parameter pairs. */
    private void sendUserWhatsApp(String recipientEmail, WhatsAppNotificationEvent event,
            String recipientName, String reference, java.util.List<String> parameters) {
        if (!whatsappNotifications.usesAiSensy()) return;
        try {
            whatsappNotifications.sendUserEvent(recipientEmail, event, recipientName, reference, parameters);
        } catch (Exception ignored) {
            // WhatsApp is never allowed to roll back the authoritative email/business action.
        }
    }

    private WhatsAppNotificationEvent orderEvent(String status) {
        String value = status == null ? "" : status.trim().toUpperCase();
        return switch (value) {
            case "CONFIRMED" -> WhatsAppNotificationEvent.ORDER_CONFIRMED;
            case "PROCESSING" -> WhatsAppNotificationEvent.ORDER_PROCESSING;
            case "PACKED" -> WhatsAppNotificationEvent.ORDER_PACKED;
            case "SHIPPED" -> WhatsAppNotificationEvent.ORDER_SHIPPED;
            case "OUT_FOR_DELIVERY" -> WhatsAppNotificationEvent.ORDER_OUT_FOR_DELIVERY;
            case "DELIVERED" -> WhatsAppNotificationEvent.ORDER_DELIVERED;
            case "CANCELLED" -> WhatsAppNotificationEvent.ORDER_CANCELLED;
            default -> WhatsAppNotificationEvent.GENERIC_NOTIFICATION;
        };
    }

    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetLink) {
        send(recipientEmail,
                "ShivHub password reset",
                "Hello " + (recipientName == null || recipientName.isBlank() ? "ShivHub user" : recipientName) + ",\n\n"
                        + "Use this secure link to reset your ShivHub password:\n\n"
                        + resetLink + "\n\n"
                        + "This link expires in 30 minutes. If you did not request this, ignore this email.\n\n"
                        + "Regards,\nShivHub Team");
        sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.PASSWORD_RESET, recipientName, "password-reset",
                java.util.List.of(recipientName == null || recipientName.isBlank() ? "ShivHub user" : recipientName, resetLink));
    }

public void sendReferralInvitation(String email, String code) {
    send(email, "ShivHub referral invitation", "You have been invited to ShivHub. Register with this email and enter referral code: " + code + ". This code can be used only once.");
    sendUserWhatsApp(email, WhatsAppNotificationEvent.REFERRAL_INVITATION, null, code,
            java.util.List.of(safeText(code)));
}

    public void sendAfterSalesStatusEmail(String recipientEmail, String customerName, String requestNumber, String status, String remarks) {
        send(recipientEmail, "ShivHub service update " + requestNumber,
                "Hello " + (customerName == null || customerName.isBlank() ? "Customer" : customerName) + ",\n\nYour after-sales request " + requestNumber
                        + " is now " + status.replace('_', ' ') + "." + (remarks == null || remarks.isBlank() ? "" : "\n\n" + remarks)
                        + "\n\nRegards,\nShivHub Team");
        sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.AFTER_SALES_UPDATED, customerName, requestNumber,
                java.util.List.of(safeText(customerName), safeText(requestNumber), safeText(status), safeText(remarks)));
    }

    /** Contact ticket acknowledgement sent only after the ticket is safely persisted. */
    public void sendContactAcknowledgement(String recipientEmail, String customerName, String ticketReference) {
        send(recipientEmail, "ShivHub support request received " + ticketReference,
                "Hello " + (customerName == null || customerName.isBlank() ? "Customer" : customerName) + ",\n\n"
                        + "We have received your support request (" + ticketReference + "). Our team will review it and work to resolve your problem as soon as possible.\n\n"
                        + "We will email you when we take an action.\n\nRegards,\nShivHub Support");
        sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.SUPPORT_TICKET_RECEIVED, customerName, ticketReference,
                java.util.List.of(safeText(customerName), safeText(ticketReference)));
    }

    /** Sends an admin-approved customer-safe update; internal support notes are intentionally excluded. */
    public void sendContactActionEmail(String recipientEmail, String customerName, String ticketReference, String status, String customerResponse) {
        send(recipientEmail, "ShivHub support update " + ticketReference,
                "Hello " + (customerName == null || customerName.isBlank() ? "Customer" : customerName) + ",\n\n"
                        + "Your support request " + ticketReference + " is now " + (status == null ? "UPDATED" : status.replace('_', ' ')) + "."
                        + (customerResponse == null || customerResponse.isBlank() ? "" : "\n\n" + customerResponse)
                        + "\n\nRegards,\nShivHub Support");
        sendCustomerWhatsApp(recipientEmail, WhatsAppNotificationEvent.SUPPORT_TICKET_UPDATED, customerName, ticketReference,
                java.util.List.of(safeText(customerName), safeText(ticketReference), safeText(status), safeText(customerResponse)));
    }

    /** Operational alert for every submitted Contact Us ticket. */
    public void sendContactAdminAlert(String adminEmail, String ticketReference, String customerName, String category, String subject) {
        send(adminEmail, "ShivHub new contact ticket " + ticketReference,
                "A new customer contact request needs review.\n\nTicket: " + ticketReference
                        + "\nCustomer: " + (customerName == null ? "Customer" : customerName)
                        + "\nCategory: " + (category == null ? "General" : category)
                        + "\nSubject: " + (subject == null ? "Support request" : subject)
                        + "\n\nOpen Admin Settings → Enquiries / Complaints to respond.");
    }


    /*
     * =========================================================
     * ORDER CONFIRMATION EMAIL
     * =========================================================
     *
     * Sent after successful order creation.
     *
     * Invoice PDF is attached.
     *
     * =========================================================
     */

    public void sendOrderConfirmationEmail(

            String customerEmail,

            String customerName,

            String orderNumber,

            String orderTotal,

            byte[] invoicePdf) {


        /*
         * =====================================================
         * DEBUG LOG
         * =====================================================
         */

        System.out.println(
                "========================================"
        );

        System.out.println(
                "SHIVHUB ORDER EMAIL"
        );

        System.out.println(
                "TO      : "
                + customerEmail
        );

        System.out.println(
                "CUSTOMER: "
                + customerName
        );

        System.out.println(
                "ORDER   : "
                + orderNumber
        );

        System.out.println(
                "AMOUNT  : "
                + orderTotal
        );

        System.out.println(
                "INVOICE : "
                + (
                    invoicePdf != null
                        ? invoicePdf.length + " bytes"
                        : "NULL"
                )
        );

        System.out.println(
                "========================================"
        );


        /*
         * =====================================================
         * SUBJECT
         * =====================================================
         */

        String subject =
                "ShivHub - Order Confirmed - "
                + orderNumber;


        /*
         * =====================================================
         * EMAIL BODY
         * =====================================================
         */

        String body =

                "Hello "
                + customerName
                + ",\n\n"

                + "Thank you for shopping with ShivHub!\n\n"

                + "Your order has been successfully placed.\n\n"

                + "Order Number: "
                + orderNumber
                + "\n\n"

                + "Order Amount: ₹"
                + orderTotal
                + "\n\n"

                + "Order Status: CONFIRMED\n\n"

                + (invoicePdf != null && invoicePdf.length > 0
                        ? "Your invoice is attached to this email.\n\n"
                        : "Your invoice will be generated after the seller selects the product IMEI/serial for dispatch.\n\n")

                + "We will keep you updated about "
                + "your order delivery status.\n\n"

                + "Thank you for choosing ShivHub.\n\n"

                + "Regards,\n"
                + "ShivHub Team";


        /*
         * =====================================================
         * SEND EMAIL
         * =====================================================
         */

        if (invoicePdf != null
                && invoicePdf.length > 0) {

            sendEmailWithAttachment(

                    customerEmail,

                    subject,

                    body,

                    invoicePdf,

                    "ShivHub-Invoice-"
                    + orderNumber
                    + ".pdf"
            );

        } else {

            /*
             * Fallback:
             *
             * Send email without attachment.
             */

            send(
                    customerEmail,
                    subject,
                    body
            );
        }
        sendCustomerWhatsApp(customerEmail, WhatsAppNotificationEvent.ORDER_PLACED, customerName, orderNumber,
                java.util.List.of(safeText(customerName), safeText(orderNumber), safeText(orderTotal), "CONFIRMED"));
    }


    /*
     * =========================================================
     * INVOICE EMAIL
     * =========================================================
     */

    public void sendInvoiceEmail(

            String customerEmail,

            String customerName,

            String orderNumber,

            byte[] invoicePdf) {


        String subject =
                "ShivHub - Invoice - "
                + orderNumber;


        String body =

                "Hello "
                + customerName
                + ",\n\n"

                + "Please find your ShivHub invoice "
                + "attached with this email.\n\n"

                + "Order Number: "
                + orderNumber
                + "\n\n"

                + "Please keep this invoice for "
                + "your records.\n\n"

                + "Thank you for shopping with ShivHub.\n\n"

                + "Regards,\n"
                + "ShivHub Team";


        sendEmailWithAttachment(

                customerEmail,

                subject,

                body,

                invoicePdf,

                "ShivHub-Invoice-"
                + orderNumber
                + ".pdf"
        );
        sendCustomerWhatsApp(customerEmail, WhatsAppNotificationEvent.INVOICE_GENERATED, customerName, orderNumber,
                java.util.List.of(safeText(customerName), safeText(orderNumber)));
    }


    /*
     * =========================================================
     * ORDER STATUS EMAIL
     * =========================================================
     */

    public void sendOrderStatusEmail(

            String customerEmail,

            String customerName,

            String orderNumber,

            String status) {
        sendOrderStatusEmail(customerEmail, customerName, orderNumber, status, null, null);
    }

    public void sendOrderStatusEmail(String customerEmail, String customerName, String orderNumber, String status,
            String deliveryPersonName, String deliveryPersonMobile) {
        sendOrderStatusEmail(customerEmail, customerName, orderNumber, status, deliveryPersonName, deliveryPersonMobile, null);
    }

    public void sendOrderStatusEmail(String customerEmail, String customerName, String orderNumber, String status,
            String deliveryPersonName, String deliveryPersonMobile, byte[] invoicePdf) {


        String subject =
                "ShivHub - Order "
                + status
                + " - "
                + orderNumber;


        String body;


        /*
         * =====================================================
         * CONFIRMED
         * =====================================================
         */

        if ("CONFIRMED".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order has been "
                    + "confirmed successfully.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: CONFIRMED\n\n"

                    + "We will start processing your "
                    + "order shortly.\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * PROCESSING
         * =====================================================
         */

        else if ("PROCESSING".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order is now "
                    + "being processed.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: PROCESSING\n\n"

                    + "Our team is preparing your order.\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }

        else if ("PACKED".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order has been packed "
                    + "and is ready for dispatch.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: PACKED\n\n"

                    + "This order can no longer be cancelled.\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * SHIPPED
         * =====================================================
         */

        else if ("SHIPPED".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Great news! Your ShivHub order "
                    + "has been shipped.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: SHIPPED\n\n"

                    + "Your order is now on its way "
                    + "to you.\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * OUT FOR DELIVERY
         * =====================================================
         */

        else if ("OUT_FOR_DELIVERY".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order is out "
                    + "for delivery.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: OUT FOR DELIVERY\n\n"

                    + "Your order should reach you soon.\n\n"

                    + "Please keep your phone available "
                    + "for the delivery partner.\n\n"
                    + "Delivery Partner: " + (deliveryPersonName == null ? "Will be shared shortly" : deliveryPersonName) + "\n"
                    + "Contact: " + (deliveryPersonMobile == null ? "Will be shared shortly" : deliveryPersonMobile) + "\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * DELIVERED
         * =====================================================
         */

        else if ("DELIVERED".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order has been "
                    + "delivered successfully.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: DELIVERED\n\n"

                    + "Thank you for shopping with ShivHub!\n\n"

                    + "We hope you enjoy your purchase.\n\n"

                    + (invoicePdf != null && invoicePdf.length > 0
                            ? "Your final invoice is attached with this email.\n\n"
                            : "You can download your invoice from My Orders in ShivHub.\n\n")

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * CANCELLED
         * =====================================================
         */

        else if ("CANCELLED".equalsIgnoreCase(status)) {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order has been "
                    + "cancelled.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "Status: CANCELLED\n\n"

                    + "If you did not request this "
                    + "cancellation, please contact "
                    + "ShivHub support.\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * OTHER STATUS
         * =====================================================
         */

        else {

            body =

                    "Hello "
                    + customerName
                    + ",\n\n"

                    + "Your ShivHub order status "
                    + "has been updated.\n\n"

                    + "Order Number: "
                    + orderNumber
                    + "\n\n"

                    + "New Status: "
                    + status
                    + "\n\n"

                    + "We will keep you updated about "
                    + "your order.\n\n"

                    + "Regards,\n"
                    + "ShivHub Team";
        }


        /*
         * =====================================================
         * SEND EMAIL
         * =====================================================
         */

        if (invoicePdf != null && invoicePdf.length > 0) {
            sendEmailWithAttachment(
                    customerEmail,
                    subject,
                    body,
                    invoicePdf,
                    "ShivHub-Invoice-" + orderNumber + ".pdf"
            );
        } else {
            send(

                    customerEmail,

                    subject,

                    body
            );
        }
        sendCustomerWhatsApp(customerEmail, orderEvent(status), customerName, orderNumber,
                java.util.List.of(safeText(customerName), safeText(orderNumber), safeText(status),
                        safeText(deliveryPersonName), safeText(deliveryPersonMobile)));
    }


    /*
     * =========================================================
     * SEND EMAIL WITH PDF ATTACHMENT
     * =========================================================
     */

    private void sendEmailWithAttachment(

            String to,

            String subject,

            String body,

            byte[] attachment,

            String attachmentName) {


        try {


            /*
             * =================================================
             * VALIDATE EMAIL
             * =================================================
             */

            if (to == null
                    || to.isBlank()) {

                throw new RuntimeException(
                        "Customer email is empty"
                );
            }


            /*
             * =================================================
             * VALIDATE ATTACHMENT
             * =================================================
             */

            if (attachment == null
                    || attachment.length == 0) {

                throw new RuntimeException(
                        "Invoice PDF is empty"
                );
            }


            /*
             * =================================================
             * DEBUG
             * =================================================
             */

            System.out.println(
                    "----------------------------------------"
            );

            System.out.println(
                    "SENDING SHIVHUB EMAIL"
            );

            System.out.println(
                    "TO         : "
                    + to
            );

            System.out.println(
                    "SUBJECT    : "
                    + subject
            );

            System.out.println(
                    "ATTACHMENT : "
                    + attachmentName
            );

            System.out.println(
                    "PDF SIZE   : "
                    + attachment.length
                    + " bytes"
            );

            System.out.println(
                    "----------------------------------------"
            );


            /*
             * =================================================
             * CREATE MIME MESSAGE
             * =================================================
             */

            MimeMessage message =
                    activeMailSender().createMimeMessage();


            /*
             * true = multipart email
             */

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true
                    );


            /*
             * =================================================
             * RECIPIENT
             * =================================================
             */

            helper.setTo(to);


            /*
             * =================================================
             * SUBJECT
             * =================================================
             */

            helper.setSubject(subject);


            /*
             * =================================================
             * BODY
             * =================================================
             */

            helper.setText(body);


            /*
             * =================================================
             * PDF ATTACHMENT
             * =================================================
             */

            InputStreamSource source =
                    () -> new ByteArrayInputStream(
                            attachment
                    );


            helper.addAttachment(
                    attachmentName,
                    source
            );


            /*
             * =================================================
             * SEND EMAIL
             * =================================================
             */

            System.out.println(
                    "Sending email through Gmail SMTP..."
            );


            activeMailSender().send(message);
            whatsappNotifications.mirrorCustomerEmail(to, subject, body);


            /*
             * =================================================
             * SUCCESS
             * =================================================
             */

            System.out.println(
                    "EMAIL SENT SUCCESSFULLY"
            );

            System.out.println(
                    "Recipient: "
                    + to
            );

            System.out.println(
                    "Order email completed."
            );

            System.out.println(
                    "----------------------------------------"
            );


        } catch (MessagingException exception) {


            /*
             * =================================================
             * EMAIL FAILURE
             * =================================================
             */

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "SHIVHUB EMAIL FAILED"
            );

            System.err.println(
                    "Recipient: "
                    + to
            );

            System.err.println(
                    "Subject: "
                    + subject
            );

            System.err.println(
                    "========================================"
            );


            exception.printStackTrace();


            throw new RuntimeException(

                    "Failed to send email with attachment",

                    exception
            );


        } catch (Exception exception) {


            /*
             * =================================================
             * OTHER EMAIL FAILURE
             * =================================================
             */

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "SHIVHUB EMAIL ERROR"
            );

            System.err.println(
                    "Recipient: "
                    + to
            );

            System.err.println(
                    "Error: "
                    + exception.getMessage()
            );

            System.err.println(
                    "========================================"
            );


            exception.printStackTrace();


            throw new RuntimeException(

                    "ShivHub email sending failed",

                    exception
            );
        }
    }
    /*
 * =========================================================
 * OFFLINE BILL EMAIL
 * =========================================================
 *
 * Sends Offline / POS bill PDF to customer.
 *
 * =========================================================
 */

public void sendOfflineBillEmail(
        String customerEmail,
        String customerName,
        String billNumber,
        String grandTotal,
        String paymentMethod,
        byte[] invoicePdf) {
    sendOfflineBillEmail(customerEmail, customerName, billNumber, grandTotal, paymentMethod, invoicePdf, true);
}

/** The POS flow passes false and sends WhatsApp only from its explicit consent snapshot. */
public void sendOfflineBillEmail(
        String customerEmail,
        String customerName,
        String billNumber,
        String grandTotal,
        String paymentMethod,
        byte[] invoicePdf,
        boolean sendWhatsApp) {


    /*
     * =====================================================
     * VALIDATION
     * =====================================================
     */

    if (customerEmail == null
            || customerEmail.isBlank()) {

        throw new RuntimeException(
                "Customer email is required"
        );
    }


    if (invoicePdf == null
            || invoicePdf.length == 0) {

        throw new RuntimeException(
                "Invoice PDF is empty"
        );
    }


    try {


        /*
         * =================================================
         * CREATE MIME MESSAGE
         * =================================================
         */

        MimeMessage message =
                activeMailSender().createMimeMessage();


        /*
         * =================================================
         * HELPER
         * =================================================
         */

        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        true
                );


        /*
         * =================================================
         * RECIPIENT
         * =================================================
         */

        helper.setTo(
                customerEmail
        );


        /*
         * =================================================
         * SUBJECT
         * =================================================
         */

        helper.setSubject(
                "ShivHub - Your Invoice "
                + billNumber
        );


        /*
         * =================================================
         * EMAIL BODY
         * =================================================
         */

        String body =
                "Hello "
                + (
                    customerName == null
                    || customerName.isBlank()
                        ? "Customer"
                        : customerName
                )
                + ",\n\n"

                + "Thank you for shopping with ShivHub.\n\n"

                + "Your purchase invoice has been "
                + "generated successfully.\n\n"

                + "Bill Number: "
                + billNumber
                + "\n"

                + "Amount: Rs. "
                + grandTotal
                + "\n"

                + "Payment Method: "
                + paymentMethod
                + "\n\n"

                + "Please find your invoice attached "
                + "with this email.\n\n"

                + "Thank you for choosing ShivHub.\n\n"

                + "Regards,\n"
                + "ShivHub Team";


        helper.setText(
                body
        );


        /*
         * =================================================
         * PDF ATTACHMENT
         * =================================================
         */

        InputStreamSource attachment =
                () -> new ByteArrayInputStream(
                        invoicePdf
                );


        helper.addAttachment(
                "ShivHub-Invoice-"
                + billNumber
                + ".pdf",
                attachment
        );


        /*
         * =================================================
         * SEND EMAIL
         * =================================================
         */

        activeMailSender().send(
                message
        );

        if (sendWhatsApp) {
            sendCustomerWhatsApp(customerEmail, WhatsAppNotificationEvent.INVOICE_GENERATED, customerName, billNumber,
                    java.util.List.of(safeText(customerName), safeText(billNumber), safeText(grandTotal), safeText(paymentMethod)));
        }

        System.out.println(
                "========================================"
        );

        System.out.println(
                "OFFLINE BILL EMAIL SENT"
        );

        System.out.println(
                "TO   : "
                + customerEmail
        );

        System.out.println(
                "BILL : "
                + billNumber
        );

        System.out.println(
                "========================================"
        );


    } catch (MessagingException exception) {


        throw new RuntimeException(
                "Could not send offline bill email",
                exception
        );
    }
}
/*
 * =========================================================
 * PURCHASE PAYMENT EMAIL
 * =========================================================
 *
 * Sends payment confirmation to Seller.
 *
 * =========================================================
 */

public void sendPurchasePaymentEmail(
        String sellerEmail,
        String sellerName,
        String invoiceNumber,
        String distributorName,
        BigDecimal paymentAmount,
        String paymentMethod,
        LocalDateTime paymentDate,
        String transactionReference,
        BigDecimal purchaseTotal,
        BigDecimal totalPaid,
        BigDecimal remainingAmount,
        String paymentStatus) {


    SimpleMailMessage message =
            new SimpleMailMessage();


    message.setTo(sellerEmail);


    message.setSubject(
            "ShivHub - Purchase Payment Confirmation"
    );


    String transaction =
            transactionReference != null
                    && !transactionReference
                            .trim()
                            .isEmpty()
                    ? transactionReference
                    : "N/A";


    message.setText(

            "Hello "
            + sellerName
            + ",\n\n"

            + "Your purchase payment has been "
            + "successfully recorded in ShivHub.\n\n"

            + "========================================\n"
            + "PURCHASE PAYMENT DETAILS\n"
            + "========================================\n\n"

            + "Invoice Number : "
            + invoiceNumber
            + "\n"

            + "Distributor    : "
            + distributorName
            + "\n"

            + "Payment Date   : "
            + paymentDate
            + "\n"

            + "Payment Method : "
            + paymentMethod
            + "\n"

            + "Transaction Ref: "
            + transaction
            + "\n\n"

            + "Payment Amount : ₹"
            + paymentAmount
            + "\n"

            + "Purchase Total : ₹"
            + purchaseTotal
            + "\n"

            + "Total Paid     : ₹"
            + totalPaid
            + "\n"

            + "Remaining Due  : ₹"
            + remainingAmount
            + "\n"

            + "Payment Status : "
            + paymentStatus
            + "\n\n"

            + "========================================\n\n"

            + "This payment has been recorded successfully "
            + "in your ShivHub account.\n\n"

            + "Thank you for using ShivHub.\n\n"

            + "Regards,\n"
            + "ShivHub Team"
    );


    activeMailSender().send(message);
    sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_PURCHASE_PAYMENT, sellerName, invoiceNumber,
            java.util.List.of(safeText(invoiceNumber), safeText(distributorName),
                    paymentAmount == null ? "" : paymentAmount.toPlainString(),
                    remainingAmount == null ? "" : remainingAmount.toPlainString(), safeText(paymentStatus)));
}

/* Seller receipt sent immediately after a distributor invoice has been saved. */
public void sendPurchaseCreatedEmail(String sellerEmail, String sellerName, String invoiceNumber,
        String distributorName, BigDecimal grandTotal, int itemCount) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(sellerEmail);
    message.setSubject("ShivHub - Purchase invoice saved: " + invoiceNumber);
    message.setText("Hello " + (sellerName == null ? "Seller" : sellerName) + ",\n\n"
            + "Your distributor purchase has been saved and stock has been updated.\n\n"
            + "Invoice: " + invoiceNumber + "\nDistributor: " + distributorName + "\n"
            + "Items received: " + itemCount + "\nTotal bill: Rs. " + grandTotal + "\n\n"
            + "Use ShivHub Purchase History to record paid and remaining amounts.\n\nRegards,\nShivHub Team");
    activeMailSender().send(message);
    sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_PURCHASE_INVOICE, sellerName, invoiceNumber,
            java.util.List.of(safeText(invoiceNumber), safeText(distributorName),
                    grandTotal == null ? "" : grandTotal.toPlainString(), String.valueOf(itemCount)));
}

public void sendLowStockAlertEmail(String sellerEmail, String sellerName, String productName, int stock) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(sellerEmail);
    message.setSubject("ShivHub low stock alert: " + productName);
    message.setText("Hello " + (sellerName == null ? "Seller" : sellerName) + ",\n\n"
            + productName + " has only " + stock + " unit(s) left. Please add a new purchase before it runs out.\n\nRegards,\nShivHub Team");
    activeMailSender().send(message);
    sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_LOW_STOCK, sellerName, productName,
            java.util.List.of(safeText(productName), String.valueOf(stock)));
}

public void sendExpenseCreatedEmail(String sellerEmail, String sellerName, String category,
        String description, BigDecimal amount, Object expenseDate, String paymentMethod, String notes) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(sellerEmail);
    message.setSubject("ShivHub - Expense added: " + category);
    message.setText("Hello " + (sellerName == null ? "Seller" : sellerName) + ",\n\n"
            + "A new expense was recorded in your ShivHub account.\n\n"
            + "Date: " + expenseDate + "\n"
            + "Category: " + category + "\n"
            + "Description: " + description + "\n"
            + "Amount: Rs. " + amount + "\n"
            + "Payment: " + (paymentMethod == null ? "-" : paymentMethod) + "\n"
            + "Notes: " + (notes == null || notes.isBlank() ? "-" : notes) + "\n\n"
            + "Regards,\nShivHub Team");
    activeMailSender().send(message);
    sendUserWhatsApp(sellerEmail, WhatsAppNotificationEvent.SELLER_EXPENSE_RECORDED, sellerName, category,
            java.util.List.of(safeText(category), safeText(description),
                    amount == null ? "" : amount.toPlainString(), safeText(String.valueOf(expenseDate))));
}

/* Distributor receives a payment record as well, so both sides have matching accounting evidence. */
public void sendDistributorPaymentEmail(String distributorEmail, String invoiceNumber, String sellerName,
        BigDecimal paymentAmount, BigDecimal totalPaid, BigDecimal remainingAmount) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(distributorEmail);
    message.setSubject("ShivHub - Payment recorded for " + invoiceNumber);
    message.setText("Hello,\n\n" + (sellerName == null ? "A ShivHub seller" : sellerName)
            + " recorded a payment against invoice " + invoiceNumber + ".\n\n"
            + "Payment received: Rs. " + paymentAmount + "\nTotal paid: Rs. " + totalPaid
            + "\nRemaining balance: Rs. " + remainingAmount + "\n\nRegards,\nShivHub Team");
    activeMailSender().send(message);
    sendUserWhatsApp(distributorEmail, WhatsAppNotificationEvent.DISTRIBUTOR_PAYMENT_RECORDED, null, invoiceNumber,
            java.util.List.of(safeText(invoiceNumber), safeText(sellerName),
                    paymentAmount == null ? "" : paymentAmount.toPlainString(),
                    remainingAmount == null ? "" : remainingAmount.toPlainString()));
}
}
