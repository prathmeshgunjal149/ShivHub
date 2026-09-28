package com.shivhub.backend.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shivhub.backend.entity.CustomerAddress;
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdOrderByIsDefaultDescUpdatedAtDesc(Long customerId);
    Optional<CustomerAddress> findByIdAndCustomerId(Long id, Long customerId);
    long countByCustomerId(Long customerId);
    @Modifying
    @Query("update CustomerAddress address set address.isDefault = false where address.customerId = :customerId and address.isDefault = true")
    void clearDefaultByCustomerId(@Param("customerId") Long customerId);
}
