package com.shivhub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MobileSpecificationRequest {

    @NotBlank(message = "Brand is required")
    @Size(max = 80, message = "Brand cannot exceed 80 characters")
    private String brand;

    @NotBlank(message = "Model name is required")
    @Size(max = 150, message = "Model name cannot exceed 150 characters")
    private String modelName;

    private String variantName;
    private String ram;
    private String storage;
    private String colorOptions;
    private String hsnCode;
    private String displayDetails;
    private String processor;
    private String cameraDetails;
    private String batteryDetails;
    private String osDetails;
    private String connectivityDetails;
    private String otherDetails;
    private Boolean indiaVariant;
    private Boolean active;
    private String source;
}
