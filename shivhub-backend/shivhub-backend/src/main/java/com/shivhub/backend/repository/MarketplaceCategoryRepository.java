package com.shivhub.backend.repository; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.shivhub.backend.entity.MarketplaceCategory;
public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategory,Long>{ List<MarketplaceCategory> findByActiveTrueOrderByNameAsc(); boolean existsByNameIgnoreCase(String name); }
