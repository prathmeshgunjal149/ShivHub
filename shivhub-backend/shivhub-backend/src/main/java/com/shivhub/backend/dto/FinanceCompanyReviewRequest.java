package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinanceCompanyReviewRequest(boolean approve,
                                          @NotBlank @Size(max = 1000) String reason) { }
