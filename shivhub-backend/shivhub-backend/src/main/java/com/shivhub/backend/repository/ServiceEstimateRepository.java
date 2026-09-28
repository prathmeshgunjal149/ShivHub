package com.shivhub.backend.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ServiceEstimate;
public interface ServiceEstimateRepository extends JpaRepository<ServiceEstimate, Long> { Optional<ServiceEstimate> findByServiceRequestId(Long serviceRequestId); }
