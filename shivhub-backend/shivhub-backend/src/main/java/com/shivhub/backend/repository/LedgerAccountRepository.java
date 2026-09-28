package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.LedgerAccount;
import com.shivhub.backend.entity.User;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    List<LedgerAccount> findBySellerOrderByCodeAsc(User seller);

    Optional<LedgerAccount> findBySellerAndCode(User seller, String code);

    boolean existsBySellerAndCode(User seller, String code);
}
