package com.shivhub.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data
public class CustomerAddressRequest {
    @NotBlank private String recipientName;
    @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10 digit Indian mobile number") private String mobileNumber;
    @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "Enter a valid alternate mobile number") private String alternateMobileNumber;
    @NotBlank private String addressLine1;
    @NotBlank private String addressLine2;
    private String landmark;
    @NotBlank private String city;
    @NotBlank private String district;
    @NotBlank private String state;
    @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Enter a valid 6 digit pincode") private String pincode;
    private String addressLabel;
}
