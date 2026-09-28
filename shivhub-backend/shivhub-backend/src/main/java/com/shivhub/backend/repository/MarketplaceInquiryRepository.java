package com.shivhub.backend.repository; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.shivhub.backend.entity.MarketplaceInquiry;
public interface MarketplaceInquiryRepository extends JpaRepository<MarketplaceInquiry,Long>{
 List<MarketplaceInquiry> findByListingOwnerIdOrderByCreatedAtDesc(Long ownerId);
 long countByListingOwnerId(Long ownerId);
 org.springframework.data.domain.Page<MarketplaceInquiry> findByStatusOrderByCreatedAtDesc(String status, org.springframework.data.domain.Pageable pageable);
 org.springframework.data.domain.Page<MarketplaceInquiry> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
