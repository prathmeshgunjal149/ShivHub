package com.shivhub.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MobileSpecificationResponse {

    private Long id;
    private String brand;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
