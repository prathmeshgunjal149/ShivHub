package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.PaymentTransaction;
import com.shivhub.backend.enums.PaymentStatus;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);
    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<PaymentTransaction> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<PaymentTransaction> findByOfflineBillIdOrderByCreatedAtDesc(Long offlineBillId);
    Optional<PaymentTransaction> findTopByOrderIdAndPaymentStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus paymentStatus);
    Optional<PaymentTransaction> findTopByOfflineBillIdAndPaymentStatusOrderByCreatedAtDesc(Long offlineBillId, PaymentStatus paymentStatus);
}
