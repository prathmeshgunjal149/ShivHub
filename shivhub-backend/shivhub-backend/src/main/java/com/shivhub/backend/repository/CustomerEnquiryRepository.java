package com.shivhub.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.CustomerEnquiry;

public interface CustomerEnquiryRepository extends JpaRepository<CustomerEnquiry, Long> {
    List<CustomerEnquiry> findTop50ByStatusOrderByCreatedAtDesc(String status);
    List<CustomerEnquiry> findTop50ByOrderByCreatedAtDesc();
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
}
