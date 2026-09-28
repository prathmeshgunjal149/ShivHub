package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.SellerCustomerMapping;
import com.shivhub.backend.entity.User;

public interface SellerCustomerMappingRepository extends JpaRepository<SellerCustomerMapping, Long> {
    List<SellerCustomerMapping> findBySeller(User seller);
    List<SellerCustomerMapping> findByCustomerProfile(CustomerProfile customerProfile);
    Optional<SellerCustomerMapping> findBySellerAndCustomerProfile(User seller, CustomerProfile customerProfile);
}
