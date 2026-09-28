package com.shivhub.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.AfterSalesStockMovement;
public interface AfterSalesStockMovementRepository extends JpaRepository<AfterSalesStockMovement,Long>{
    boolean existsByServiceRequestIdAndProductVariantId(Long serviceRequestId, Long productVariantId);
    java.util.Optional<AfterSalesStockMovement> findByServiceRequestIdAndProductVariantId(Long requestId, Long variantId);
    @org.springframework.data.jpa.repository.Query("""
        select coalesce(sum(m.quantity),0) from AfterSalesStockMovement m
        where m.productVariantId = :variantId
        and ((:orderItemId is not null and m.serviceRequest.orderItemId = :orderItemId)
          or (:billItemId is not null and m.serviceRequest.offlineBillItemId = :billItemId))
        """)
    long disposedQuantity(@org.springframework.data.repository.query.Param("variantId") Long variantId,
        @org.springframework.data.repository.query.Param("orderItemId") Long orderItemId,
        @org.springframework.data.repository.query.Param("billItemId") Long billItemId);
}
