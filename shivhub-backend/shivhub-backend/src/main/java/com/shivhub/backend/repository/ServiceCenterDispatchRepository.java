package com.shivhub.backend.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ServiceCenterDispatch;
public interface ServiceCenterDispatchRepository extends JpaRepository<ServiceCenterDispatch, Long> { Optional<ServiceCenterDispatch> findByServiceRequestId(Long serviceRequestId); }
