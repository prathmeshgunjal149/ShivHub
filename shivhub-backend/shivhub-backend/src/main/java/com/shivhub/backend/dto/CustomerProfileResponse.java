package com.shivhub.backend.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponse {

    private Long id;

    private String name;

    private String email;

    private String mobile;

    private String profilePhotoUrl;

    private boolean marketingOptOut;

    private LocalDate dateOfBirth;

    private Role role;

    private UserStatus status;

    private boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
