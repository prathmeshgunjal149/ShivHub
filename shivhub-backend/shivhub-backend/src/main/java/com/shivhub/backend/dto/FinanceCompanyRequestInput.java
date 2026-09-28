package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinanceCompanyRequestInput(@NotBlank @Size(max = 150) String name,
                                         @Size(max = 500) String contact,
                                         @Size(max = 1000) String remarks) { }
