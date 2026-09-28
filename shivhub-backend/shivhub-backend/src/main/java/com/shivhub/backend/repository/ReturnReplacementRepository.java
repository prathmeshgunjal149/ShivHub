package com.shivhub.backend.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ReturnReplacement;
public interface ReturnReplacementRepository extends JpaRepository<ReturnReplacement, Long> { Optional<ReturnReplacement> findByServiceRequestId(Long serviceRequestId); }
