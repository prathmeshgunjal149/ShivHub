package com.shivhub.backend.repository;

import com.shivhub.backend.entity.FinanceSale;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FinanceSaleRepository extends JpaRepository<FinanceSale, Long>, JpaSpecificationExecutor<FinanceSale> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FinanceSale f where f.id = :id")
    Optional<FinanceSale> lock(@Param("id") Long id);
    Optional<FinanceSale> findByBillId(Long id);
    @Query("select f.id from FinanceSale f where f.active = true and f.summarySent = false order by f.id")
    List<Long> summaries(Pageable page);
}
