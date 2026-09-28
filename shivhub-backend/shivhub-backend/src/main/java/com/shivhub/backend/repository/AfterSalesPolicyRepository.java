package com.shivhub.backend.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.AfterSalesPolicy;
public interface AfterSalesPolicyRepository extends JpaRepository<AfterSalesPolicy, Long> {
    Optional<AfterSalesPolicy> findFirstByProductIdAndActiveTrueOrderByIdDesc(Long productId);
    Optional<AfterSalesPolicy> findFirstByCategoryIdAndProductIdIsNullAndActiveTrueOrderByIdDesc(Long categoryId);
    Optional<AfterSalesPolicy> findFirstByCategoryIdIsNullAndProductIdIsNullAndActiveTrueOrderByIdDesc();
    List<AfterSalesPolicy> findByActiveTrueOrderByIdDesc();
}
