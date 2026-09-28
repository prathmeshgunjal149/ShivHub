package com.shivhub.backend.repository;

import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerDistributorRepository
        extends JpaRepository<SellerDistributor, Long> {

    // All distributors connected with a seller
    List<SellerDistributor> findBySeller(User seller);

    // Active distributors of a seller
    List<SellerDistributor> findBySellerAndActiveTrue(User seller);

    // All sellers connected with a distributor
    List<SellerDistributor> findByDistributor(Distributor distributor);

    // Check whether seller already has this distributor for this brand
    boolean existsBySellerAndDistributorAndBrandIgnoreCase(
            User seller,
            Distributor distributor,
            String brand
    );

    // Find a particular seller-distributor-brand relationship
    Optional<SellerDistributor> findBySellerAndDistributorAndBrandIgnoreCase(
            User seller,
            Distributor distributor,
            String brand
    );

    // Get distributors of a particular brand for a seller
    List<SellerDistributor> findBySellerAndBrandIgnoreCase(
            User seller,
            String brand
    );

    // Get active distributors of a particular brand for a seller
    List<SellerDistributor> findBySellerAndBrandIgnoreCaseAndActiveTrue(
            User seller,
            String brand
    );

    // Check relationship by IDs
    boolean existsBySellerIdAndDistributorIdAndBrandIgnoreCase(
            Long sellerId,
            Long distributorId,
            String brand
    );

    // Get seller-distributor relationships by seller ID
    List<SellerDistributor> findBySellerId(Long sellerId);

    // Get active seller-distributor relationships by seller ID
    List<SellerDistributor> findBySellerIdAndActiveTrue(Long sellerId);

    @Query("""
            select link from SellerDistributor link join fetch link.distributor distributor
            where link.seller = :seller and link.active = true and upper(distributor.status) = 'ACTIVE' and (
                 lower(distributor.businessName) like lower(concat('%', :query, '%'))
              or lower(coalesce(distributor.mobile, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(distributor.email, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(distributor.gstin, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(link.brand, '')) like lower(concat('%', :query, '%')))
            order by case when lower(distributor.businessName) like lower(concat(:query, '%')) then 0 else 1 end,
                     distributor.businessName asc
            """)
    List<SellerDistributor> searchActiveSuggestions(@Param("seller") User seller, @Param("query") String query, Pageable pageable);
}
