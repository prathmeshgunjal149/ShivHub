package com.shivhub.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.InstantMobileBill;
import com.shivhub.backend.entity.User;

public interface InstantMobileBillRepository extends JpaRepository<InstantMobileBill, Long> {
    List<InstantMobileBill> findBySellerAndCreatedAtBetweenOrderByCreatedAtDesc(User seller, LocalDateTime from, LocalDateTime to);
    Optional<InstantMobileBill> findBySellerAndId(User seller, Long id);
}
