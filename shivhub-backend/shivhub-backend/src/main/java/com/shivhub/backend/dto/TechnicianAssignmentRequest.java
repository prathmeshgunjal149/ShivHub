package com.shivhub.backend.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class TechnicianAssignmentRequest { @NotNull private Long technicianId; private String remarks; }
