package com.shivhub.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.FinancialYear;
import com.shivhub.backend.entity.User;

public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {

    List<FinancialYear> findBySellerOrderByStartDateDesc(User seller);

    Optional<FinancialYear> findBySellerAndYearCode(User seller, String yearCode);

    @Query("""
            select year from FinancialYear year
            where year.seller = :seller
              and :date between year.startDate and year.endDate
            """)
    Optional<FinancialYear> findForDate(@Param("seller") User seller, @Param("date") LocalDate date);
}
