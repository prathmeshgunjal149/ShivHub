package com.shivhub.backend.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerProfileRequest {

    private String name;

    private String mobile;

    private String profilePhotoUrl;

    private LocalDate dateOfBirth;
}
