package com.shivhub.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ServicePart;
public interface ServicePartRepository extends JpaRepository<ServicePart, Long> { List<ServicePart> findByServiceRequestId(Long serviceRequestId); void deleteByServiceRequestId(Long serviceRequestId); }
