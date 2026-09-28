package com.shivhub.backend.service;

import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.ProductVariantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** All variant stock changes lock the parent first, then the selected unit. */
@Service
public class VariantInventoryService {
    private final ProductVariantRepository variants;
    private final EntityManager entityManager;
    private final StockMovementService movements;
    public VariantInventoryService(ProductVariantRepository variants, EntityManager entityManager, StockMovementService movements) {
        this.variants = variants; this.entityManager = entityManager; this.movements = movements;
    }
    @Transactional
    public ProductVariant selected(Product product, Long variantId) {
        if (!product.isVariantsEnabled()) fail("This product uses its existing stock flow");
        if (variantId == null) fail("Please select all product options");
        entityManager.flush();
        entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
        ProductVariant v = variants.findLocked(variantId, product.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant does not belong to product"));
        if (!v.isActive()) fail("Selected variant is unavailable");
        return v;
    }
    @Transactional
    public ProductVariant reserve(Product product, Long variantId, int quantity) {
        ProductVariant v = selected(product, variantId);
        validateQuantity(quantity);
        if (v.getAvailableStock() < quantity) fail("Selected variant is out of stock or has insufficient quantity");
        v.setReservedQuantity(v.getReservedQuantity() + quantity);
        synchronize(product); return v;
    }
    @Transactional
    public void release(Product product, Long variantId, int quantity) {
        ProductVariant v = selected(product, variantId); validateQuantity(quantity);
        if (v.getReservedQuantity() < quantity) fail("Variant reservation is inconsistent; contact support");
        v.setReservedQuantity(v.getReservedQuantity() - quantity); synchronize(product);
    }
    @Transactional
    public ProductVariant sell(Product product, Long variantId, int quantity, boolean reserved, String referenceType, Long referenceId) {
        ProductVariant v = selected(product, variantId); validateQuantity(quantity);
        if (reserved ? v.getReservedQuantity() < quantity || v.getStockQuantity() < quantity : v.getAvailableStock() < quantity) fail("Selected variant has insufficient stock");
        int before = product.getStock();
        v.setStockQuantity(v.getStockQuantity() - quantity);
        if (reserved) v.setReservedQuantity(v.getReservedQuantity() - quantity);
        synchronize(product);
        StockMovement movement = movements.recordMovement(product, product.getSeller(), "SALE", -quantity, before, product.getStock(), referenceType, referenceId, "Variant " + v.getId() + " " + v.getAttributesJson());
        movement.setProductVariantId(v.getId());
        return v;
    }
    private void synchronize(Product product) {
        variants.flush();
        var all = variants.findByProductOrderByIdAsc(product);
        product.setStock(all.stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::getStockQuantity).sum());
        product.setReservedStock(all.stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::getReservedQuantity).sum());
    }
    private void validateQuantity(int quantity) { if (quantity < 1) fail("Quantity must be at least one"); }
    private void fail(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
