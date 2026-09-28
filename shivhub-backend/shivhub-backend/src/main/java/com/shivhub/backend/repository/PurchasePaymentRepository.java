package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchasePayment;
import com.shivhub.backend.entity.User;

public interface PurchasePaymentRepository
        extends JpaRepository<PurchasePayment, Long> {

    List<PurchasePayment> findByPurchaseOrderByPaymentDateDesc(
            Purchase purchase
    );

    long countByPurchase(Purchase purchase);

    @Query("""
            select payment from PurchasePayment payment
            join fetch payment.purchase purchase
            where purchase.seller = :seller
            order by payment.paymentDate desc
            """)
    List<PurchasePayment> findBySeller(@Param("seller") User seller);
}
