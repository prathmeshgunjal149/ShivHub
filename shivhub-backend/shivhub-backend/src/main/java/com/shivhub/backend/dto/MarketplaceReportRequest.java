package com.shivhub.backend.dto; import jakarta.validation.constraints.*; import lombok.Data;
@Data public class MarketplaceReportRequest { @NotBlank @Size(max=80) private String reason; @Size(max=3000) private String details; }
