package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.shivhub.backend.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


/*
 * =========================================================
 * CreateOfflineBillRequest
 * =========================================================
 *
 * Request received from Seller POS when creating an
 * offline shop bill.
 *
 * =========================================================
 */

public class CreateOfflineBillRequest {
    @Valid
    private FinanceRequest finance;
    public FinanceRequest getFinance() { return finance; }
    public void setFinance(FinanceRequest finance) { this.finance = finance; }


    /*
     * =========================================================
     * CUSTOMER
     * =========================================================
     *
     * customerId can be null for walk-in customers.
     *
     */

    private Long customerId;

    /** Canonical POS profile for seller-only and online customers; verified against customerMobile server-side. */
    private Long customerProfileId;


    /*
     * Customer name is required because even a walk-in
     * customer should appear on the invoice.
     */

    @NotBlank(message = "Customer name is required")
    private String customerName;


    /*
     * Customer mobile
     */

    private String customerMobile;

    /** Explicit counter consent for transactional WhatsApp invoice and order updates. */
    private boolean whatsappConsent;


    /*
     * Customer email
     */

    private String customerEmail;

    private String customerAddress;


    /*
     * Customer GSTIN
     */

    private String customerGstin;

    /** Optional B2B invoice identity.  Kept separate from the customer profile
     * because a walk-in may request a one-off GST invoice. */
    private String customerLegalName;
    private String customerTradeName;
    private String placeOfSupply;


    /*
     * =========================================================
     * PRODUCTS
     * =========================================================
     */

    @Valid
    @NotEmpty(message = "At least one product is required")
    private List<OfflineBillItemRequest> items =
            new ArrayList<>();


    /*
     * =========================================================
     * BILL DISCOUNT
     * =========================================================
     */

    private BigDecimal discount = BigDecimal.ZERO;

    /** Requested points are validated and consumed only after the sale is fully paid. */
    private Integer loyaltyPointsToRedeem = 0;


    /*
     * =========================================================
     * DELIVERY CHARGE
     * =========================================================
     *
     * Normally zero for shop billing.
     *
     */

    private BigDecimal deliveryCharge =
            BigDecimal.ZERO;


    /*
     * =========================================================
     * PAYMENT METHOD
     * =========================================================
     */

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;


    /*
     * =========================================================
     * PAYMENT AMOUNT
     * =========================================================
     */

    private BigDecimal paymentAmount =
            BigDecimal.ZERO;


    /*
     * =========================================================
     * TRANSACTION ID
     * =========================================================
     *
     * Used for UPI/Card/Bank Transfer.
     *
     */

    private String transactionId;


    /*
     * =========================================================
     * NOTES
     * =========================================================
     */

    private String notes;

    /* Optional staff member who completed this counter sale. */
    private Long salesPersonId;

    /* Customer promise date when the sale is partially paid. */
    private LocalDate balanceDueDate;


    /*
     * Default constructor
     */

    public CreateOfflineBillRequest() {
    }


    /*
     * =========================================================
     * GETTERS / SETTERS
     * =========================================================
     */

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getCustomerProfileId() { return customerProfileId; }

    public void setCustomerProfileId(Long customerProfileId) { this.customerProfileId = customerProfileId; }


    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }


    public String getCustomerMobile() {
        return customerMobile;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public boolean isWhatsappConsent() { return whatsappConsent; }

    public void setWhatsappConsent(boolean whatsappConsent) { this.whatsappConsent = whatsappConsent; }


    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerAddress() { return customerAddress; }

    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }


    public String getCustomerGstin() {
        return customerGstin;
    }

    public void setCustomerGstin(String customerGstin) {
        this.customerGstin = customerGstin;
    }

    public String getCustomerLegalName() { return customerLegalName; }
    public void setCustomerLegalName(String customerLegalName) { this.customerLegalName = customerLegalName; }
    public String getCustomerTradeName() { return customerTradeName; }
    public void setCustomerTradeName(String customerTradeName) { this.customerTradeName = customerTradeName; }
    public String getPlaceOfSupply() { return placeOfSupply; }
    public void setPlaceOfSupply(String placeOfSupply) { this.placeOfSupply = placeOfSupply; }


    public List<OfflineBillItemRequest> getItems() {
        return items;
    }

    public void setItems(
            List<OfflineBillItemRequest> items) {

        this.items = items;
    }


    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public Integer getLoyaltyPointsToRedeem() { return loyaltyPointsToRedeem; }

    public void setLoyaltyPointsToRedeem(Integer loyaltyPointsToRedeem) { this.loyaltyPointsToRedeem = loyaltyPointsToRedeem; }


    public BigDecimal getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(
            BigDecimal deliveryCharge) {

        this.deliveryCharge = deliveryCharge;
    }


    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(
            PaymentMethod paymentMethod) {

        this.paymentMethod = paymentMethod;
    }


    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(
            BigDecimal paymentAmount) {

        this.paymentAmount = paymentAmount;
    }


    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(
            String transactionId) {

        this.transactionId = transactionId;
    }


    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getSalesPersonId() { return salesPersonId; }

    public void setSalesPersonId(Long salesPersonId) { this.salesPersonId = salesPersonId; }

    public LocalDate getBalanceDueDate() { return balanceDueDate; }

    public void setBalanceDueDate(LocalDate balanceDueDate) { this.balanceDueDate = balanceDueDate; }
}
