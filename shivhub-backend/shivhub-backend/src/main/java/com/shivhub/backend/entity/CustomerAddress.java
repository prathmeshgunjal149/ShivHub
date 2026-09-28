package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_addresses")
@Data
public class CustomerAddress {
    @JsonIgnore @Column(precision=10,scale=7) private java.math.BigDecimal latitude;
    @JsonIgnore @Column(precision=10,scale=7) private java.math.BigDecimal longitude;
    @JsonIgnore @Column(length=64) private String geocodedAddressHash;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long customerId;
    @Column(nullable = false, length = 120) private String recipientName;
    @Column(nullable = false, length = 10) private String mobileNumber;
    @Column(length = 10) private String alternateMobileNumber;
    @Column(nullable = false, length = 255) private String addressLine1;
    @Column(nullable = false, length = 255) private String addressLine2;
    @Column(length = 255) private String landmark;
    @Column(nullable = false, length = 120) private String city;
    @Column(nullable = false, length = 120) private String district;
    @Column(nullable = false, length = 120) private String state;
    @Column(nullable = false, length = 6) private String pincode;
    @Column(nullable = false, length = 20) private String addressLabel = "HOME";
    /*
     * Keep the public JSON contract as "isDefault".  A primitive field named
     * isDefault otherwise follows JavaBean naming and can be serialised as
     * "default", which breaks the reusable address UI.
     */
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    @JsonIgnore @Column(nullable = false, name = "is_default") private boolean isDefault;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    @JsonIgnore public boolean isDefault() { return isDefault; }
    @JsonProperty("isDefault") public boolean getDefaultAddress() { return isDefault; }
    @JsonProperty("isDefault") public void setDefault(boolean defaultAddress) { isDefault = defaultAddress; }
}
