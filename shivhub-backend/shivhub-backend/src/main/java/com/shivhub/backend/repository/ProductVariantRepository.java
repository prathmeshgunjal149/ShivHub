package com.shivhub.backend.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductVariant;
public interface ProductVariantRepository extends JpaRepository<ProductVariant,Long> {
    List<ProductVariant> findByProductSellerAndBarcodeIgnoreCase(com.shivhub.backend.entity.User seller, String barcode);
    List<ProductVariant> findByProductSellerAndVariantSkuIgnoreCase(com.shivhub.backend.entity.User seller, String sku);
    List<ProductVariant> findByProductAndActiveTrueOrderByIdAsc(Product product);
    List<ProductVariant> findByProductOrderByIdAsc(Product product);
    Optional<ProductVariant> findByIdAndProduct(Long id, Product product);
    boolean existsByProduct(Product product);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);
    boolean existsByVariantSkuIgnoreCaseAndProductSellerIdAndIdNot(String sku, Long sellerId, Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ProductVariant v where v.id=:id and v.product.id=:productId")
    Optional<ProductVariant> findLocked(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("productId") Long productId);
}
