package com.shivhub.backend.service;

/**
 * Stable business events used to resolve an AiSensy API campaign at runtime.
 * Campaign names remain configuration, not source code, because AiSensy requires
 * a live API campaign for each approved template.
 */
public enum WhatsAppNotificationEvent {
    GENERIC_NOTIFICATION("generic-notification"),
    ORDER_PLACED("order-placed"),
    ORDER_CONFIRMED("order-confirmed"),
    ORDER_PROCESSING("order-processing"),
    ORDER_PACKED("order-packed"),
    ORDER_SHIPPED("order-shipped"),
    ORDER_OUT_FOR_DELIVERY("order-out-for-delivery"),
    ORDER_DELIVERED("order-delivered"),
    ORDER_CANCELLED("order-cancelled"),
    INVOICE_GENERATED("invoice-generated"),
    PAYMENT_SUCCESS("payment-success"),
    COUPON_APPLIED("coupon-applied"),
    PAYMENT_REMINDER("payment-reminder"),
    REFUND_COMPLETED("refund-completed"),
    DELIVERY_TIME_UPDATED("delivery-time-updated"),
    LOYALTY_POINTS_UPDATED("loyalty-points-updated"),
    AFTER_SALES_UPDATED("after-sales-updated"),
    SUPPORT_TICKET_RECEIVED("support-ticket-received"),
    SUPPORT_TICKET_UPDATED("support-ticket-updated"),
    MARKETPLACE_LISTING_SUBMITTED("marketplace-listing-submitted"),
    MARKETPLACE_LISTING_APPROVED("marketplace-listing-approved"),
    MARKETPLACE_LISTING_REJECTED("marketplace-listing-rejected"),
    MARKETPLACE_ENQUIRY("marketplace-enquiry"),
    MARKETPLACE_ITEM_SOLD("marketplace-item-sold"),
    MARKETPLACE_LISTING_EXPIRED("marketplace-listing-expired"),
    MARKETPLACE_LISTING_ARCHIVED("marketplace-listing-archived"),
    EMI_SCHEDULE("emi-schedule"),
    EMI_REMINDER("emi-reminder"),
    EMI_OVERDUE("emi-overdue"),
    OFFER_NOTIFICATION("offer-notification"),
    OFFER_IMAGE_NOTIFICATION("offer-image-notification"),
    SELLER_ACCOUNT_APPROVED("seller-account-approved"),
    SELLER_ACCOUNT_REJECTED("seller-account-rejected"),
    SELLER_PRODUCT_APPROVED("seller-product-approved"),
    SELLER_PRODUCT_REJECTED("seller-product-rejected"),
    SELLER_PAYMENT_REMINDER("seller-payment-reminder"),
    SELLER_PURCHASE_INVOICE("seller-purchase-invoice"),
    SELLER_PURCHASE_PAYMENT("seller-purchase-payment"),
    SELLER_LOW_STOCK("seller-low-stock"),
    SELLER_EXPENSE_RECORDED("seller-expense-recorded"),
    SELLER_SUBSCRIPTION_UPDATED("seller-subscription-updated"),
    STAFF_ACCOUNT_ASSIGNED("staff-account-assigned"),
    DISTRIBUTOR_WORKFLOW_UPDATED("distributor-workflow-updated"),
    DISTRIBUTOR_PAYMENT_RECORDED("distributor-payment-recorded"),
    REFERRAL_INVITATION("referral-invitation"),
    OTP_REGISTRATION("otp-registration"),
    OTP_LOGIN("otp-login"),
    PASSWORD_RESET("password-reset");

    private final String configKey;

    WhatsAppNotificationEvent(String configKey) { this.configKey = configKey; }

    public String configKey() { return configKey; }
}
