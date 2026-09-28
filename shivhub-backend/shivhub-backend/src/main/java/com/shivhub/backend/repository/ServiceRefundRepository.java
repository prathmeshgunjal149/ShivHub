package com.shivhub.backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import com.shivhub.backend.entity.ServiceRefund;
import com.shivhub.backend.enums.ServiceRefundStatus;
public interface ServiceRefundRepository extends JpaRepository<ServiceRefund, Long> {
 List<ServiceRefund> findByServiceRequestIdOrderByInitiatedAtDesc(Long serviceRequestId);
 long countByServiceRequestIdAndRefundStatus(Long serviceRequestId, ServiceRefundStatus status);
 @Query("select coalesce(sum(f.refundAmount), 0) from ServiceRefund f join f.serviceRequest r where r.orderItemId = :itemId and f.refundStatus = com.shivhub.backend.enums.ServiceRefundStatus.COMPLETED")
 BigDecimal completedForOrderItem(@Param("itemId") Long itemId);
 @Query("select coalesce(sum(f.refundAmount), 0) from ServiceRefund f join f.serviceRequest r where r.offlineBillItemId = :itemId and f.refundStatus = com.shivhub.backend.enums.ServiceRefundStatus.COMPLETED")
 BigDecimal completedForOfflineBillItem(@Param("itemId") Long itemId);
}
