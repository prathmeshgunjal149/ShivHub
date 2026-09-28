package com.shivhub.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.ShopRegisterOpeningCash;
import com.shivhub.backend.entity.User;

public interface ShopRegisterOpeningCashRepository extends JpaRepository<ShopRegisterOpeningCash, Long> {
    Optional<ShopRegisterOpeningCash> findBySellerAndBusinessDate(User seller, LocalDate businessDate);
    List<ShopRegisterOpeningCash> findBySellerAndBusinessDateBetweenOrderByBusinessDateAsc(User seller, LocalDate from, LocalDate to);
}
