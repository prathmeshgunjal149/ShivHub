package com.shivhub.backend.dto;

import com.shivhub.backend.enums.StaffAccessRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Owner-only request for creating a staff account and assigning it to a branch. */
public record CreateShopStaffRequest(
        @NotBlank(message = "Staff name is required") String name,
        @NotBlank(message = "Staff email is required") @Email String email,
        @NotBlank(message = "Mobile number is required") @Pattern(regexp = "^[6-9][0-9]{9}$") String mobile,
        @NotBlank(message = "Temporary password is required") @Size(min = 8) String temporaryPassword,
        @NotNull(message = "Staff access role is required") StaffAccessRole accessRole
) {}
