package com.shivhub.backend.repository;

import com.shivhub.backend.entity.FinanceScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinanceSchemeRepository extends JpaRepository<FinanceScheme, Long> {
    List<FinanceScheme> findByActiveTrueOrderByNameAsc();
}
