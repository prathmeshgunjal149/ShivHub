package com.shivhub.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.ShopRegisterEntry;
import com.shivhub.backend.entity.User;

public interface ShopRegisterEntryRepository extends JpaRepository<ShopRegisterEntry, Long> {
    List<ShopRegisterEntry> findBySellerAndActiveTrueAndEntryAtBetweenOrderByEntryAtDesc(User seller, LocalDateTime from, LocalDateTime to);
}
