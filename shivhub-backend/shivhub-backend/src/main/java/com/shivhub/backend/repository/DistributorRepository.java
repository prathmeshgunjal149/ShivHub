package com.shivhub.backend.repository;

import com.shivhub.backend.entity.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    Optional<Distributor> findByGstin(String gstin);

    boolean existsByGstin(String gstin);

    List<Distributor> findByStatus(String status);

    List<Distributor> findBySubmittedBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Distributor> findByBusinessNameContainingIgnoreCase(String businessName);

    @Query("""
            select distributor from Distributor distributor
            where lower(distributor.businessName) like lower(concat('%', :query, '%'))
               or lower(coalesce(distributor.mobile, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(distributor.email, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(distributor.gstin, '')) like lower(concat('%', :query, '%'))
            order by case when lower(distributor.businessName) like lower(concat(:query, '%')) then 0 else 1 end,
                     distributor.businessName asc
            """)
    List<Distributor> searchSuggestions(@Param("query") String query, Pageable pageable);
}
