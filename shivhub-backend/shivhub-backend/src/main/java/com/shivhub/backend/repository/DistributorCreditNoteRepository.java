package com.shivhub.backend.repository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import com.shivhub.backend.entity.*;
public interface DistributorCreditNoteRepository extends JpaRepository<DistributorCreditNote, Long> {
 List<DistributorCreditNote> findBySellerOrderByCreditNoteDateDesc(User seller);
 @Query("select coalesce(sum(c.amount), 0) from DistributorCreditNote c where c.purchase = :purchase") BigDecimal totalForPurchase(Purchase purchase);
}
