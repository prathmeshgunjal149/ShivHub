package com.shivhub.backend.dto;

import com.shivhub.backend.enums.LedgerAccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LedgerAccountRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    @NotNull private LedgerAccountType accountType;
}
