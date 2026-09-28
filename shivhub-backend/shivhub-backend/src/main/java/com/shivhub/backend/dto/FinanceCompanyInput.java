package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinanceCompanyInput(@NotBlank @Size(max = 150) String name,
                                  @Size(max = 500) String contact,
                                  boolean active) { }
