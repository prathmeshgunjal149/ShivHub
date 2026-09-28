package com.shivhub.backend.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** POS-only profile fields. It intentionally never accepts a password or role. */
@Data
public class SellerCustomerUpsertRequest {
    @NotBlank private String name;
    @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10 digit mobile number") private String mobile;
    private String email;
    private String address;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private LocalDate dateOfBirth;
    private boolean whatsappConsent;
}
