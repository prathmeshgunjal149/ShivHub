package com.shivhub.backend.repository;

import com.shivhub.backend.entity.EmiInstallment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmiInstallmentRepository extends JpaRepository<EmiInstallment, Long> {
    List<EmiInstallment> findBySaleIdOrderByInstallmentNumber(Long sale);
    @Query("select e.id from EmiInstallment e where e.status = 'ACTIVE' and e.sale.active = true and e.reminderSent = false and e.dueDate = :due and e.id > :after order by e.id")
    List<Long> due(@Param("due") LocalDate due, @Param("after") Long after, Pageable page);
    @Query("select e.id from EmiInstallment e where e.status = 'ACTIVE' and e.sale.active = true and e.reminderSent = false and e.failureMessage is not null and e.dueDate >= :today and e.id > :after order by e.id")
    List<Long> failedBeforeDue(@Param("today") LocalDate today, @Param("after") Long after, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmiInstallment e where e.id = :id")
    Optional<EmiInstallment> lock(@Param("id") Long id);
}
