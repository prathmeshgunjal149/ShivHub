package com.shivhub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributorResponse {

    private Long id;

    private String businessName;

    private String contactPerson;

    private String mobile;

    private String email;

    private String distributorCode;
    private String businessType;
    private String alternateMobile;
    private String whatsappNumber;
    private String website;

    private String gstin;
    private String gstRegistrationStatus;
    private String pan;

    private String address;

    private String city;

    private String state;

    private String pincode;
    private String district;
    private String stateCode;
    private String billingAddress;
    private String warehouseAddress;
    private String accountsEmail;

    private String brands;
    private String productCategories;
    private String distributorType;
    private String authorizationDetails;
    private String salespersonName;
    private String salespersonMobile;
    private String claimsContact;
    private String bankDetails;
    private String accountHolderName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String branchName;
    private String accountType;
    private String upiId;
    private String paymentQrUrl;
    private String documentUrls;
    private String returnPolicy;
    private String replacementTerms;
    private String shortageReportingPeriod;
    private String warrantyClaimProcess;
    private String creditNoteTerms;
    private String deliveryTerms;
    private String usualDeliveryTime;
    private String schemeTerms;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long submittedBySellerId;

    private String adminReview;
}
