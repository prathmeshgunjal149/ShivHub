package com.shivhub.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data public class ServiceReceiveRequest { @NotBlank @Size(max=5000) private String receivingCondition; private String customerVisibleRemarks; }
