package com.shivhub.backend.dto; import jakarta.validation.constraints.*; import lombok.Data;
@Data public class MarketplaceInquiryRequest { @NotBlank @Size(max=2000) private String message; }
