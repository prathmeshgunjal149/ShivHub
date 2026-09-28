package com.shivhub.backend.dto;

import com.shivhub.backend.enums.StaffAccessRole;
import jakarta.validation.constraints.NotNull;

/** Owner/manager selects an existing staff account and its access level for this shop. */
public record ShopStaffAssignmentRequest(
        @NotNull(message = "Staff user ID is required") Long staffUserId,
        @NotNull(message = "Staff access role is required") StaffAccessRole accessRole
) {}
