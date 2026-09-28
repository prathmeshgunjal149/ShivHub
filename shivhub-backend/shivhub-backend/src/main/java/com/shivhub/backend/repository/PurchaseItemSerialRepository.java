package com.shivhub.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;

public interface PurchaseItemSerialRepository
        extends JpaRepository<PurchaseItemSerial, Long> {


    /*
     * =========================================================
     * CHECK IMEI EXISTS ANYWHERE
     * =========================================================
     */

    @Query("""
            select case
                when count(serial) > 0 then true
                else false
            end
            from PurchaseItemSerial serial
            where serial.imei1 = :imei
               or serial.imei2 = :imei
            """)
    boolean existsImeiAnywhere(
            @Param("imei") String imei
    );


    /*
     * =========================================================
     * AVAILABLE IMEI FOR SELLER + PRODUCT
     * =========================================================
     *
     * IMPORTANT:
     *
     * status = AVAILABLE
     * AND
     * soldOfflineBillItem IS NULL
     *
     * Both checks are used.
     * =========================================================
     */

    @Query("""
            select serial
            from PurchaseItemSerial serial

            join fetch serial.purchaseItem item

            join fetch item.purchase purchase

            where item.product.id = :productId

              and purchase.seller.id = :sellerId

              and purchase.status =
                  com.shivhub.backend.enums.PurchaseStatus.COMPLETED

              and serial.status = 'AVAILABLE'

              and serial.soldOfflineBillItem is null

              and serial.reservedOrderItem is null

              and serial.soldOrderItem is null

              and (
                    serial.imei1 is not null
                    or serial.imei2 is not null
                    or serial.serialNumber is not null
                  )

            order by serial.id
            """)
    List<PurchaseItemSerial> findAvailableBySellerAndProduct(
            @Param("sellerId") Long sellerId,
            @Param("productId") Long productId
    );


    /*
     * =========================================================
     * VERIFY SELECTED IMEI FOR SALE
     * =========================================================
     */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select serial
            from PurchaseItemSerial serial

            join fetch serial.purchaseItem item

            join fetch item.purchase purchase

            where serial.id in :serialIds

              and purchase.seller.id = :sellerId

              and item.product.id = :productId

              and purchase.status =
                  com.shivhub.backend.enums.PurchaseStatus.COMPLETED

              and serial.status = 'AVAILABLE'

              and serial.soldOfflineBillItem is null

              and serial.reservedOrderItem is null

              and serial.soldOrderItem is null
            """)
    List<PurchaseItemSerial> findAvailableForSale(
            @Param("sellerId") Long sellerId,
            @Param("productId") Long productId,
            @Param("serialIds") List<Long> serialIds
    );

    /** Exact scanner lookup only; never use a partial IMEI/serial match for billing. */
    @Query("""
            select serial
            from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            join fetch item.purchase purchase
            join fetch item.product product
            where serial.imei1 = :code
               or serial.imei2 = :code
               or serial.serialNumber = :code
            order by serial.id
            """)
    List<PurchaseItemSerial> findByExactScanCode(@Param("code") String code);


    /*
     * =========================================================
     * FIND IMEI HISTORY
     * =========================================================
     */

    Optional<PurchaseItemSerial> findByImei1OrImei2(
            String imei1,
            String imei2
    );


    /*
     * =========================================================
     * FIND SERIALS OF PURCHASE ITEM
     * =========================================================
     */

    List<PurchaseItemSerial>
    findByPurchaseItemIdOrderByIdAsc(
            Long purchaseItemId
    );

    @Query("""
            select serial from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            where serial.soldOfflineBillItem.id = :billItemId
            order by serial.id asc
            """)
    List<PurchaseItemSerial> findSoldForOfflineBillItem(@Param("billItemId") Long billItemId);

    @Query("""
            select serial from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            where serial.reservedOfflineBillItem.id = :billItemId
            order by serial.id asc
            """)
    List<PurchaseItemSerial> findReservedForOfflineBillItem(@Param("billItemId") Long billItemId);

    @Query("""
            select serial
            from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            join fetch item.purchase purchase
            where serial.id in :serialIds
              and purchase.seller.id = :sellerId
              and item.product.id = :productId
              and purchase.status = com.shivhub.backend.enums.PurchaseStatus.COMPLETED
              and serial.status = 'AVAILABLE'
              and serial.soldOfflineBillItem is null
              and serial.reservedOrderItem is null
              and serial.soldOrderItem is null
            """)
    List<PurchaseItemSerial> findAvailableForOnlineOrder(
            @Param("sellerId") Long sellerId,
            @Param("productId") Long productId,
            @Param("serialIds") List<Long> serialIds
    );

    @Query("""
            select serial
            from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            where serial.reservedOrderItem.order.id = :orderId
            order by serial.id asc
            """)
    List<PurchaseItemSerial> findReservedForOrder(@Param("orderId") Long orderId);

    @Query("""
            select serial
            from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            where serial.reservedOrderItem.id = :orderItemId
               or serial.soldOrderItem.id = :orderItemId
            order by serial.id asc
            """)
    List<PurchaseItemSerial> findAssignedForOrderItem(@Param("orderItemId") Long orderItemId);


    /*
     * =========================================================
     * FIND SERIALS OF PRODUCT
     * =========================================================
     */

    List<PurchaseItemSerial>
    findByPurchaseItem_Product_IdOrderByIdAsc(
            Long productId
    );

    @Query("""
            select serial from PurchaseItemSerial serial
            join fetch serial.purchaseItem item
            join fetch item.purchase purchase
            left join fetch serial.soldOfflineBillItem soldItem
            left join fetch soldItem.offlineBill
            where purchase.seller = :seller
            order by serial.id desc
            """)
    List<PurchaseItemSerial> findByPurchaseItemPurchaseSeller(
            @Param("seller") User seller
    );
}
