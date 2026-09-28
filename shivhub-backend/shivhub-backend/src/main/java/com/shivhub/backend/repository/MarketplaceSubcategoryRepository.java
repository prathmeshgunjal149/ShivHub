package com.shivhub.backend.repository; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.shivhub.backend.entity.MarketplaceSubcategory;
public interface MarketplaceSubcategoryRepository extends JpaRepository<MarketplaceSubcategory,Long>{ List<MarketplaceSubcategory> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId); }
