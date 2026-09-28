package com.shivhub.backend.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record SellerExpenseRequest(@NotBlank String category, @NotBlank String description, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull LocalDate expenseDate, String paymentMethod, String notes) {}
