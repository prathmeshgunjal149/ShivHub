package com.shivhub.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ServiceStatusHistory;
public interface ServiceStatusHistoryRepository extends JpaRepository<ServiceStatusHistory, Long> {
    List<ServiceStatusHistory> findByServiceRequestIdOrderByChangedAtAsc(Long serviceRequestId);
}
