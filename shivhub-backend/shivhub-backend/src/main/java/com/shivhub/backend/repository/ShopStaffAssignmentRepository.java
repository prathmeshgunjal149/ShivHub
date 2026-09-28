package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.Shop;
import com.shivhub.backend.entity.ShopStaffAssignment;
import com.shivhub.backend.entity.User;

public interface ShopStaffAssignmentRepository extends JpaRepository<ShopStaffAssignment, Long> {
    List<ShopStaffAssignment> findByShopOrderByCreatedAtDesc(Shop shop);
    Optional<ShopStaffAssignment> findByShopAndStaff(Shop shop, User staff);
    List<ShopStaffAssignment> findByStaffAndActiveTrue(User staff);
    List<ShopStaffAssignment> findByShopOwnerAndActiveTrue(User owner);
}
