package com.shivhub.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.ProductRepository;

/** Finalizes inventory that was held for a pending Razorpay POS collection. */
@Service
public class OfflineBillPaymentFulfillmentService {

    private final OfflineBillRepository bills;
    private final ProductRepository products;
    private final OfflineBillImeiService imeiService;
    @org.springframework.beans.factory.annotation.Autowired private VariantInventoryService variantInventory;

    public OfflineBillPaymentFulfillmentService(OfflineBillRepository bills,
                                                ProductRepository products,
                                                OfflineBillImeiService imeiService) {
        this.bills = bills;
        this.products = products;
        this.imeiService = imeiService;
    }

    @Transactional
    public void finalizeIfPending(OfflineBill bill) {
        if (bill == null || !Boolean.TRUE.equals(bill.getInventoryPending())) return;

        for (OfflineBillItem item : bill.getItems()) {
            Product product = products.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Product is no longer available for this paid bill"));
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (item.getProductVariantId() != null) {
                variantInventory.sell(product, item.getProductVariantId(), quantity, true, "OFFLINE_VARIANT_BILL_ITEM", item.getId());
                continue;
            }
            int currentStock = product.getStock() == null ? 0 : product.getStock();
            if (quantity <= 0 || currentStock < quantity) {
                throw new IllegalStateException("Reserved stock cannot be finalized safely");
            }
            product.setStock(currentStock - quantity);
            int reserved = product.getReservedStock() == null ? 0 : product.getReservedStock();
            product.setReservedStock(Math.max(0, reserved - quantity));
            products.save(product);
            imeiService.finalizeOfflineBillReservation(item);
        }

        bill.setInventoryPending(false);
        bill.setStockFinalizedAt(LocalDateTime.now());
        bills.save(bill);
    }
}
