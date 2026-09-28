package com.shivhub.backend.dto; import jakarta.validation.constraints.*; import lombok.Data;
@Data public class MarketplaceDecisionRequest { @NotNull private Boolean approved; @Size(max=2000) private String remarks; }
