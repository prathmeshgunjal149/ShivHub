package com.shivhub.backend.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class EstimateDecisionRequest { @NotNull private Boolean approved; private String remarks; }
