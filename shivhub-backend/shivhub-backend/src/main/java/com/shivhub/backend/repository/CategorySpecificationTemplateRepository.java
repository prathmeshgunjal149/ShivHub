package com.shivhub.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shivhub.backend.entity.CategorySpecificationTemplate;

public interface CategorySpecificationTemplateRepository extends JpaRepository<CategorySpecificationTemplate, Long> {
    boolean existsBySubCategoryId(Long subCategoryId);
    @Query("select t from CategorySpecificationTemplate t where t.active=true and t.category.id=:categoryId and (:subCategoryId is null and t.subCategory is null or :subCategoryId is not null and (t.subCategory is null or t.subCategory.id=:subCategoryId)) order by t.displayOrder asc, t.id asc")
    List<CategorySpecificationTemplate> findActiveForCategory(@Param("categoryId") Long categoryId, @Param("subCategoryId") Long subCategoryId);
    List<CategorySpecificationTemplate> findAllByOrderByCategoryIdAscSubCategoryIdAscDisplayOrderAsc();
}
