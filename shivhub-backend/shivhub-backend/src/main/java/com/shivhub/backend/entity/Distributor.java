package com.shivhub.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "distributors",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "gstin")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Distributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Distributor / Company Name
    @Column(nullable = false, length = 150)
    private String businessName;

    // Person responsible at distributor
    @Column(length = 100)
    private String contactPerson;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Column(name = "distributor_code", length = 50)
    private String distributorCode;

    @Column(name = "business_type", length = 80)
    private String businessType;

    @Column(name = "alternate_mobile", length = 15)
    private String alternateMobile;

    @Column(name = "whatsapp_number", length = 15)
    private String whatsappNumber;

    @Column(length = 180)
    private String website;

    // GSTIN
    @Column(unique = true, length = 15)
    private String gstin;

    @Column(name = "gst_registration_status", length = 40)
    private String gstRegistrationStatus;

    @Column(length = 20)
    private String pan;

    // Complete address
    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(length = 100)
    private String district;

    @Column(name = "state_code", length = 10)
    private String stateCode;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "warehouse_address", length = 500)
    private String warehouseAddress;

    @Column(name = "accounts_email", length = 150)
    private String accountsEmail;

    @Column(name = "brands", length = 500)
    private String brands;

    @Column(name = "product_categories", length = 500)
    private String productCategories;

    @Column(name = "distributor_type", length = 80)
    private String distributorType;

    @Column(name = "authorization_details", columnDefinition = "TEXT")
    private String authorizationDetails;

    @Column(name = "salesperson_name", length = 100)
    private String salespersonName;

    @Column(name = "salesperson_mobile", length = 15)
    private String salespersonMobile;

    @Column(name = "claims_contact", length = 150)
    private String claimsContact;

    @Column(name = "bank_details", columnDefinition = "TEXT")
    private String bankDetails;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "branch_name", length = 120)
    private String branchName;

    @Column(name = "account_type", length = 60)
    private String accountType;

    @Column(name = "upi_id", length = 120)
    private String upiId;

    @Column(name = "payment_qr_url", length = 500)
    private String paymentQrUrl;

    @Column(name = "document_urls", columnDefinition = "TEXT")
    private String documentUrls;

    @Column(name = "return_policy", columnDefinition = "TEXT")
    private String returnPolicy;

    @Column(name = "replacement_terms", columnDefinition = "TEXT")
    private String replacementTerms;

    @Column(name = "shortage_reporting_period", length = 100)
    private String shortageReportingPeriod;

    @Column(name = "warranty_claim_process", columnDefinition = "TEXT")
    private String warrantyClaimProcess;

    @Column(name = "credit_note_terms", columnDefinition = "TEXT")
    private String creditNoteTerms;

    @Column(name = "delivery_terms", columnDefinition = "TEXT")
    private String deliveryTerms;

    @Column(name = "usual_delivery_time", length = 100)
    private String usualDeliveryTime;

    @Column(name = "scheme_terms", columnDefinition = "TEXT")
    private String schemeTerms;

    // ACTIVE / INACTIVE
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /** Seller who submitted this distributor; null means it was created directly by an admin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_seller_id")
    @JsonIgnore
    private User submittedBySeller;

    @Column(columnDefinition = "TEXT")
    private String adminReview;

    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
