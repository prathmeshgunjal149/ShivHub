package com.shivhub.backend.entity;

import java.time.LocalDateTime;
import com.shivhub.backend.enums.AfterSalesAttachmentType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_attachments", indexes = @Index(name = "idx_service_attachment_request", columnList = "service_request_id"))
@Data
public class ServiceAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false) private ServiceRequest serviceRequest;
    @Enumerated(EnumType.STRING) @Column(name = "attachment_type", nullable = false, length = 30) private AfterSalesAttachmentType attachmentType;
    @Column(name = "file_url", nullable = false, length = 2000) private String fileUrl;
    @Column(name = "uploaded_by_user_id", nullable = false) private Long uploadedByUserId;
    @Column(name = "uploaded_at", nullable = false) private LocalDateTime uploadedAt;
    @PrePersist void onCreate() { if (uploadedAt == null) uploadedAt = LocalDateTime.now(); }
}
