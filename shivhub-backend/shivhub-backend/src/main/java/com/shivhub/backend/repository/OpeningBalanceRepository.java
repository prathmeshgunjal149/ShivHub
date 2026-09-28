package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.FinancialYear;
import com.shivhub.backend.entity.OpeningBalance;
import com.shivhub.backend.entity.User;

public interface OpeningBalanceRepository extends JpaRepository<OpeningBalance, Long> {

    List<OpeningBalance> findBySellerAndFinancialYearOrderByIdAsc(User seller, FinancialYear financialYear);

    List<OpeningBalance> findBySellerAndFinancialYearAndPostedFalse(User seller, FinancialYear financialYear);
}
