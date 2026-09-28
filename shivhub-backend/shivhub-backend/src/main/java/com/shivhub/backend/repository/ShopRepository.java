package com.shivhub.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.Shop;
import com.shivhub.backend.entity.User;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByOwnerOrderByNameAsc(User owner);
    boolean existsByOwnerAndShopCode(User owner, String shopCode);
}
