package com.shivhub.backend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shivhub.backend.entity.ServiceRequest;
import com.shivhub.backend.enums.ServiceRequestStatus;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ServiceRequest r where r.id = :id")
    Optional<ServiceRequest> findLockedById(@Param("id") Long id);
    Optional<ServiceRequest> findByRequestNumber(String requestNumber);
    List<ServiceRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ServiceRequest> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);
    boolean existsByOrderItemIdAndStatusNotIn(Long orderItemId, Collection<ServiceRequestStatus> closedStatuses);
    boolean existsByOfflineBillItemIdAndStatusNotIn(Long offlineBillItemId, Collection<ServiceRequestStatus> closedStatuses);
    long countBySellerIdAndStatus(Long sellerId, ServiceRequestStatus status);
    long countByStatus(ServiceRequestStatus status);
    @Query("select r from ServiceRequest r where r.sellerId = :sellerId and (:status is null or r.status = :status) order by r.createdAt desc")
    List<ServiceRequest> findSellerQueue(@Param("sellerId") Long sellerId, @Param("status") ServiceRequestStatus status);
    @Query("select r from ServiceRequest r where (:status is null or r.status = :status) and (:from is null or r.createdAt >= :from) and (:to is null or r.createdAt < :to) order by r.createdAt desc")
    List<ServiceRequest> findAdminQueue(@Param("status") ServiceRequestStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
