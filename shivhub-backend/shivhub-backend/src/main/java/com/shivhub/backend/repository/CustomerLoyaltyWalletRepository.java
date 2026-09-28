package com.shivhub.backend.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shivhub.backend.entity.CustomerLoyaltyWallet;

public interface CustomerLoyaltyWalletRepository extends JpaRepository<CustomerLoyaltyWallet, Long> {
    Optional<CustomerLoyaltyWallet> findByCustomerProfileId(Long customerProfileId);

    /** Prevent two checkout sessions from redeeming the same points balance concurrently. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from CustomerLoyaltyWallet wallet where wallet.customerProfile.id = :customerProfileId")
    Optional<CustomerLoyaltyWallet> findByCustomerProfileIdForUpdate(@Param("customerProfileId") Long customerProfileId);
}
