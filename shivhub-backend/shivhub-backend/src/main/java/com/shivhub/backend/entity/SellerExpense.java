package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seller_expenses")
@Data
public class SellerExpense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "seller_id", nullable = false) private User seller;
    @Column(nullable = false, length = 80) private String category;
    @Column(nullable = false, length = 160) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "expense_date", nullable = false) private LocalDate expenseDate;
    @Column(length = 40) private String paymentMethod;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created() { if (expenseDate == null) expenseDate = LocalDate.now(); createdAt = LocalDateTime.now(); }
}
