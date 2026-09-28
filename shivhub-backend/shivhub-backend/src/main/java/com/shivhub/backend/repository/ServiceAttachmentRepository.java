package com.shivhub.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ServiceAttachment;
public interface ServiceAttachmentRepository extends JpaRepository<ServiceAttachment, Long> {
    List<ServiceAttachment> findByServiceRequestIdOrderByUploadedAtAsc(Long serviceRequestId);
}
