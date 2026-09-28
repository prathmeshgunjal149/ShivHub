package com.shivhub.backend.repository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.shivhub.backend.entity.CustomerReceivable;
import com.shivhub.backend.entity.CustomerReceivablePayment;
import com.shivhub.backend.entity.User;
public interface CustomerReceivablePaymentRepository extends JpaRepository<CustomerReceivablePayment, Long> {
 List<CustomerReceivablePayment> findByReceivableOrderByPaymentDateDesc(CustomerReceivable receivable);
 @Query("select coalesce(sum(p.amount), 0) from CustomerReceivablePayment p where p.receivable = :receivable") BigDecimal totalPaid(CustomerReceivable receivable);
 @Query("""
        select payment from CustomerReceivablePayment payment
        join fetch payment.receivable receivable
        where receivable.seller = :seller
        order by payment.paymentDate desc
        """)
 List<CustomerReceivablePayment> findBySeller(@Param("seller") User seller);
}
