package com.shivhub.backend.dto;
import java.time.LocalDateTime;
import com.shivhub.backend.enums.AfterSalesAttachmentType;
import lombok.Data;
@Data public class ServiceAttachmentResponse { private Long id; private AfterSalesAttachmentType attachmentType; private String fileUrl; private LocalDateTime uploadedAt; }
