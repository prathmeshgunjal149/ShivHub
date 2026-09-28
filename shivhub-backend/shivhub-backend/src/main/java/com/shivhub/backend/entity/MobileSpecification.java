package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "mobile_specifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mobile_spec_brand_model_variant",
                columnNames = {"brand", "model_name", "variant_name"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String brand;

    @Column(name = "model_name", nullable = false, length = 150)
    private String modelName;

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(length = 50)
    private String ram;

    @Column(length = 50)
    private String storage;

    @Column(name = "color_options", length = 500)
    private String colorOptions;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode = "85171300";

    @Column(name = "display_details", columnDefinition = "TEXT")
    private String displayDetails;

    @Column(length = 200)
    private String processor;

    @Column(name = "camera_details", columnDefinition = "TEXT")
    private String cameraDetails;

    @Column(name = "battery_details", columnDefinition = "TEXT")
    private String batteryDetails;

    @Column(name = "os_details", length = 200)
    private String osDetails;

    @Column(name = "connectivity_details", columnDefinition = "TEXT")
    private String connectivityDetails;

    @Column(name = "other_details", columnDefinition = "TEXT")
    private String otherDetails;

    @Column(name = "india_variant", nullable = false)
    private Boolean indiaVariant = true;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 80)
    private String source = "MANUAL";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) active = true;
        if (indiaVariant == null) indiaVariant = true;
        if (source == null || source.isBlank()) source = "MANUAL";
        if (hsnCode == null || hsnCode.isBlank()) hsnCode = "85171300";
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
