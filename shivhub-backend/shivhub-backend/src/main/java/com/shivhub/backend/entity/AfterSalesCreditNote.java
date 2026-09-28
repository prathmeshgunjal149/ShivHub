package com.shivhub.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

/** Append-only GST credit-note snapshot; the historical invoice is never edited. */
@Entity
@Table(name="after_sales_credit_notes", uniqueConstraints=@UniqueConstraint(name="uk_after_sales_credit_note_request",columnNames="service_request_id"))
@Data
public class AfterSalesCreditNote {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_request_id",nullable=false) private ServiceRequest serviceRequest;
 @Column(name="credit_note_number",nullable=false,unique=true,length=80) private String creditNoteNumber;
 @Column(name="original_reference",nullable=false,length=120) private String originalReference;
 @Column(name="taxable_amount",nullable=false,precision=14,scale=2) private BigDecimal taxableAmount;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal cgst;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal sgst;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal igst;
 @Column(name="grand_total",nullable=false,precision=14,scale=2) private BigDecimal grandTotal;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
