package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.StockTransferAdjustment;
import com.shivhub.backend.entity.User;

public interface StockTransferAdjustmentRepository extends JpaRepository<StockTransferAdjustment, Long> {

    List<StockTransferAdjustment> findBySellerOrderByCreatedAtDesc(User seller);

    List<StockTransferAdjustment> findBySellerAndStatusOrderByCreatedAtDesc(User seller, String status);
}
