package com.shivhub.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "emi_installments", uniqueConstraints = @UniqueConstraint(columnNames = {"finance_sale_id", "installmentNumber"}), indexes = @Index(name = "idx_emi_due", columnList = "dueDate,reminderSent"))
@Getter
@Setter
public class EmiInstallment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "finance_sale_id", nullable = false) private FinanceSale sale;
    private int installmentNumber;
    @Column(nullable = false, length = 40) private String monthLabel;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDate dueDate;
    @Column(length = 20) private String status = "ACTIVE";
    private boolean reminderSent;
    private LocalDateTime reminderSentAt;
    @Column(length = 250) private String failureMessage;
}
