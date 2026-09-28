package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ProductStatus;


/*
 * =========================================================
 * ProductRepository
 * =========================================================
 *
 * Communicates with:
 *
 *     products
 *
 * table in MySQL.
 *
 * Used for:
 *
 * 1. Saving products
 * 2. Finding products
 * 3. Finding seller products
 * 4. Finding active products
 * 5. Finding pending products
 * 6. Admin approval flow
 *
 * =========================================================
 */

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    /** Loads review data with image URLs in one read for the admin preview. */
    @EntityGraph(attributePaths = { "images", "seller" })
    Optional<Product> findDetailedById(Long id);
    @Query("select p from Product p where p.active=true and p.approvalStatus=com.shivhub.backend.enums.ProductStatus.APPROVED and p.seller.id=:sellerId and p.categoryEntity.id=:categoryId and lower(p.brand)=:brand and lower(p.model)=:model and p.productType=:productType and p.physicalCondition=:condition order by p.id")
    List<Product> findMobileSiblingOptions(@Param("sellerId")Long sellerId,@Param("categoryId")Long categoryId,@Param("brand")String brand,@Param("model")String model,@Param("productType")String productType,@Param("condition")String condition,org.springframework.data.domain.Pageable pageable);


    /*
     * =====================================================
     * GET PRODUCTS OF A SELLER
     * =====================================================
     *
     * Example:
     *
     * Seller ID = 3
     *
     * Returns only products belonging to seller 3.
     *
     * =====================================================
     */

    List<Product> findBySeller(
            User seller
    );


    /*
     * =====================================================
     * GET ACTIVE PRODUCTS
     * =====================================================
     *
     * This method can be used when we only want products
     * where active = true.
     *
     * =====================================================
     */

    List<Product> findByActiveTrue();


    /*
     * =====================================================
     * GET ACTIVE + APPROVED PRODUCTS
     * =====================================================
     *
     * CUSTOMER PRODUCT LISTING USES THIS.
     *
     * Product must satisfy BOTH:
     *
     * active = true
     *
     * AND
     *
     * approvalStatus = APPROVED
     *
     * This prevents:
     *
     * PENDING products
     * REJECTED products
     *
     * from appearing to customers.
     *
     * =====================================================
     */

    List<Product> findByActiveTrueAndApprovalStatus(
            ProductStatus approvalStatus
    );

    List<Product> findTop30ByApprovalStatusAndActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(
            ProductStatus approvalStatus,
            String name
    );

    List<Product> findTop30ByApprovalStatusAndActiveTrueAndBrandContainingIgnoreCaseOrderByUpdatedAtDesc(
            ProductStatus approvalStatus,
            String brand
    );

    List<Product> findTop30ByApprovalStatusAndActiveTrueOrderByUpdatedAtDesc(
            ProductStatus approvalStatus
    );

    Optional<Product> findBySellerAndCatalogueParent(User seller, Product catalogueParent);

    @Query("""
            select product from Product product
            left join fetch product.categoryEntity
            left join fetch product.subCategory
            where product.active = true and product.approvalStatus = :status
              and (lower(product.name) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.brand, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.model, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.sellerSku, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.barcode, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.category, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.ram, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.storage, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.colorOptions, '')) like lower(concat('%', :query, '%')))
            order by case when lower(product.name) = lower(:query) then 0
                          when lower(product.name) like lower(concat(:query, '%')) then 1
                          when lower(coalesce(product.brand, '')) like lower(concat(:query, '%')) then 2
                          else 3 end, product.name asc
            """)
    List<Product> searchPublicSuggestions(@Param("status") ProductStatus status, @Param("query") String query, Pageable pageable);

    @Query("""
            select product from Product product
            left join fetch product.categoryEntity
            left join fetch product.subCategory
            where product.seller = :seller
              and (lower(product.name) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.brand, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.model, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.sellerSku, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.barcode, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.category, '')) like lower(concat('%', :query, '%')))
            order by case when lower(product.name) = lower(:query) then 0
                          when lower(product.name) like lower(concat(:query, '%')) then 1 else 2 end, product.name asc
            """)
    List<Product> searchSellerSuggestions(@Param("seller") User seller, @Param("query") String query, Pageable pageable);

    /**
     * Paginated purchase catalogue search.  This intentionally lives in the
     * database: a seller with a large catalogue must never have every product
     * downloaded simply to receive stock against one product.
     */
    @EntityGraph(attributePaths = { "categoryEntity", "subCategory" })
    @Query(value = """
            select product from Product product
            left join product.categoryEntity categoryEntity
            left join product.subCategory subCategory
            where product.seller = :seller
              and (:categoryId is null or categoryEntity.id = :categoryId)
              and (:subCategoryId is null or subCategory.id = :subCategoryId)
              and (:query = ''
                or lower(product.name) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.brand, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.model, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.sellerSku, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.barcode, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(categoryEntity.name, product.category, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(subCategory.name, '')) like lower(concat('%', :query, '%')))
            order by case when lower(product.name) = lower(:query) then 0
                          when lower(product.name) like lower(concat(:query, '%')) then 1 else 2 end,
                     product.name asc
            """,
            countQuery = """
            select count(product) from Product product
            left join product.categoryEntity categoryEntity
            left join product.subCategory subCategory
            where product.seller = :seller
              and (:categoryId is null or categoryEntity.id = :categoryId)
              and (:subCategoryId is null or subCategory.id = :subCategoryId)
              and (:query = ''
                or lower(product.name) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.brand, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.model, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.sellerSku, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(product.barcode, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(categoryEntity.name, product.category, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(subCategory.name, '')) like lower(concat('%', :query, '%')))
            """)
    Page<Product> searchSellerPurchaseCatalogue(
            @Param("seller") User seller,
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("subCategoryId") Long subCategoryId,
            Pageable pageable);

    @Query("""
            select product from Product product
            left join fetch product.categoryEntity
            left join fetch product.subCategory
            where lower(product.name) like lower(concat('%', :query, '%'))
               or lower(coalesce(product.brand, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(product.model, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(product.sellerSku, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(product.barcode, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(product.category, '')) like lower(concat('%', :query, '%'))
            order by case when lower(product.name) = lower(:query) then 0
                          when lower(product.name) like lower(concat(:query, '%')) then 1
                          when lower(coalesce(product.brand, '')) like lower(concat(:query, '%')) then 2 else 3 end,
                     product.name asc
            """)
    List<Product> searchAdminSuggestions(@Param("query") String query, Pageable pageable);

    Optional<Product> findByBarcodeIgnoreCase(String barcode);

    Optional<Product> findFirstBySellerAndBarcodeIgnoreCase(User seller, String barcode);

    List<Product> findBySellerAndSellerSkuIgnoreCase(User seller, String sellerSku);


    /*
     * =====================================================
     * GET PRODUCTS BY APPROVAL STATUS
     * =====================================================
     *
     * Used by Admin.
     *
     * Example:
     *
     * ProductStatus.PENDING
     *
     * returns all products waiting for Admin review.
     *
     * =====================================================
     */

    List<Product> findByApprovalStatus(
            ProductStatus approvalStatus
    );


    /*
     * =====================================================
     * GET SELLER PRODUCTS BY STATUS
     * =====================================================
     *
     * Useful for Seller Dashboard.
     *
     * Example:
     *
     * PENDING
     * APPROVED
     * REJECTED
     *
     * =====================================================
     */

    List<Product> findBySellerAndApprovalStatus(
            User seller,
            ProductStatus approvalStatus
    );


    /* Dashboard queries are kept here so the service does not need to load every product. */
    long countBySeller(User seller);

    long countBySellerAndActiveTrue(User seller);

    long countBySellerAndApprovalStatus(User seller, ProductStatus approvalStatus);

    long countBySellerAndStockLessThanEqual(User seller, Integer stock);

    List<Product> findTop6BySellerOrderByUpdatedAtDesc(User seller);

    List<Product> findTop6BySellerAndStockLessThanEqualOrderByStockAscUpdatedAtDesc(
            User seller,
            Integer stock
    );
}
