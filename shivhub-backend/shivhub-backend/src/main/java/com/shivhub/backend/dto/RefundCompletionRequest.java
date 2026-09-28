package com.shivhub.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data public class RefundCompletionRequest { @NotBlank @Size(max=120) private String transactionReference; }
