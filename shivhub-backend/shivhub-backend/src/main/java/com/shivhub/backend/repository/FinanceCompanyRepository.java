package com.shivhub.backend.repository;

import com.shivhub.backend.entity.FinanceCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinanceCompanyRepository extends JpaRepository<FinanceCompany, Long> {
    List<FinanceCompany> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
