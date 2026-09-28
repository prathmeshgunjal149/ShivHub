package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributorRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name cannot exceed 150 characters")
    private String businessName;

    @Size(max = 100, message = "Contact person cannot exceed 100 characters")
    private String contactPerson;

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 15, message = "Mobile number must be between 10 and 15 characters")
    private String mobile;

    @Email(message = "Invalid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    private String distributorCode;
    private String businessType;
    private String alternateMobile;
    private String whatsappNumber;
    private String website;
    private String gstRegistrationStatus;
    private String pan;
    private String district;
    private String stateCode;
    private String billingAddress;
    private String warehouseAddress;
    private String accountsEmail;

    @Size(max = 15, message = "GSTIN cannot exceed 15 characters")
    private String gstin;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 10, message = "Pincode cannot exceed 10 characters")
    private String pincode;

    @Size(max = 500, message = "Brands cannot exceed 500 characters")
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
}
