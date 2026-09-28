package com.shivhub.backend.repository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.AfterSalesCreditNote;
public interface AfterSalesCreditNoteRepository extends JpaRepository<AfterSalesCreditNote,Long>{
 Optional<AfterSalesCreditNote> findByServiceRequestId(Long requestId);
 @Query("select n from AfterSalesCreditNote n join n.serviceRequest r where r.sellerId=:sellerId and n.createdAt>=:start and n.createdAt<:end")
 List<AfterSalesCreditNote> findSellerNotesBetween(@Param("sellerId") Long sellerId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
