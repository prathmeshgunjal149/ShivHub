package com.shivhub.backend.entity;

import java.time.LocalDateTime;

import com.shivhub.backend.enums.StaffAccessRole;
import jakarta.persistence.*;
import lombok.Data;

/** Links a staff user to exactly the branch and access role assigned by the owner. */
@Entity
@Table(name = "shop_staff_assignments", uniqueConstraints = @UniqueConstraint(name = "uk_shop_staff", columnNames = {"shop_id", "staff_id"}))
@Data
public class ShopStaffAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StaffAccessRole accessRole;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
