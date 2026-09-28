package com.shivhub.backend.repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.SellerExpense;
import com.shivhub.backend.entity.User;
public interface SellerExpenseRepository extends JpaRepository<SellerExpense, Long> {
    List<SellerExpense> findBySellerAndExpenseDateBetweenOrderByExpenseDateDesc(User seller, LocalDate from, LocalDate to);
}
