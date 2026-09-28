package com.shivhub.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "policy_pages")
@Data
@NoArgsConstructor
public class PolicyPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 300)
    private String description;

    @Column(name = "published_content", columnDefinition = "TEXT")
    private String publishedContent;

    @Column(name = "draft_content", columnDefinition = "TEXT")
    private String draftContent;

    @Column
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private boolean published = true;

    @Column(nullable = false)
    private boolean demoData = true;

    @Column(nullable = false)
    private boolean requiresReview = true;

    @Column
    private LocalDateTime lastPublishedAt;

    @Column
    private Long lastPublishedByAdminId;

    @Column
    private LocalDateTime lastEditedAt;

    @Column
    private Long lastEditedByAdminId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (lastPublishedAt == null && published) lastPublishedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
