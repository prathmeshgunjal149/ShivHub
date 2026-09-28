package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.PurchaseItemRequest;
import com.shivhub.backend.dto.PurchaseRequest;
import com.shivhub.backend.dto.PurchaseProductSearchResponse;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.StockMovement;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.ProductVariant;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.SellerDistributorRepository;
import com.shivhub.backend.repository.StockMovementRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.ProductVariantRepository;


@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;

    private final PurchaseItemRepository purchaseItemRepository;

    private final ProductRepository productRepository;

    private final SellerDistributorRepository sellerDistributorRepository;

    private final StockMovementRepository stockMovementRepository;

    private final PurchaseItemSerialRepository purchaseItemSerialRepository;

    private final EmailService emailService;

    private final ProductConfigurationService productConfigurationService;
    private final ProductVariantRepository productVariantRepository;

    /* Explicit constructor keeps Spring injection and VS Code Java analysis
     * independent of Lombok annotation processing. */
    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            ProductRepository productRepository,
            SellerDistributorRepository sellerDistributorRepository,
            StockMovementRepository stockMovementRepository,
            PurchaseItemSerialRepository purchaseItemSerialRepository,
            EmailService emailService,
            ProductConfigurationService productConfigurationService,
            ProductVariantRepository productVariantRepository) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.productRepository = productRepository;
        this.sellerDistributorRepository = sellerDistributorRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.purchaseItemSerialRepository = purchaseItemSerialRepository;
        this.emailService = emailService;
        this.productConfigurationService = productConfigurationService;
        this.productVariantRepository = productVariantRepository;
    }

    /** Database-side seller catalogue search for the purchase screen. */
    @Transactional(readOnly = true)
    public Page<PurchaseProductSearchResponse> searchSellerProducts(
            User seller, String query, Long categoryId, Long subCategoryId, int page, int size) {
        validateSeller(seller);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        String safeQuery = query == null ? "" : query.trim();
        return productRepository.searchSellerPurchaseCatalogue(seller, safeQuery, categoryId, subCategoryId,
                        PageRequest.of(safePage, safeSize))
                .map(this::purchaseSearchResponse);
    }

    /** Scanner validation is exact and never treats a partial code as an IMEI. */
    @Transactional(readOnly = true)
    public Map<String, Object> validateIncomingImei(User seller, Long productId, String code) {
        validateSeller(seller);
        if (productId == null) throw new RuntimeException("Select a mobile product before scanning its IMEI");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (product.getSeller() == null || !seller.getId().equals(product.getSeller().getId()))
            throw new RuntimeException("Product does not belong to this seller");
        if (!ProductConfigurationService.isMobile(product))
            throw new RuntimeException("IMEI scanning is available only for mobile purchase items");

        String imei = normalizeImei(code);
        if (imei == null) throw new RuntimeException("IMEI is required");
        List<PurchaseItemSerial> matches = purchaseItemSerialRepository.findByExactScanCode(imei);
        if (!matches.isEmpty()) {
            PurchaseItemSerial existing = matches.get(0);
            User owner = existing.getPurchaseItem().getPurchase().getSeller();
            if (owner == null || !seller.getId().equals(owner.getId()))
                throw new RuntimeException("This IMEI belongs to another seller");
            if ("SOLD".equalsIgnoreCase(existing.getStatus()))
                throw new RuntimeException("This IMEI is already sold and cannot be received again");
            throw new RuntimeException("This IMEI already exists in ShivHub");
        }
        return Map.of("valid", true, "code", imei);
    }

    private PurchaseProductSearchResponse purchaseSearchResponse(Product product) {
        Long categoryId = product.getCategoryEntity() == null ? null : product.getCategoryEntity().getId();
        Long subCategoryId = product.getSubCategory() == null ? null : product.getSubCategory().getId();
        return new PurchaseProductSearchResponse(
                product.getId(), product.getName(), product.getBrand(), product.getModel(),
                product.getRam(), product.getStorage(), product.getColorOptions(), product.getSellerSku(),
                product.getBarcode(), product.getHsnCode(), product.getPrice(), product.getPrice(),
                product.getGstRate(), product.getStock(), categoryId,
                product.getCategoryEntity() == null ? product.getCategory() : product.getCategoryEntity().getName(),
                subCategoryId, product.getSubCategory() == null ? null : product.getSubCategory().getName(),
                ProductConfigurationService.isMobile(product),
                product.isVariantsEnabled(),
                productConfigurationService.read(product.getProductSpecifications()));
    }


    /*
     * =========================================================
     * CREATE PURCHASE
     * =========================================================
     */

    @Transactional
    public Purchase createPurchase(
            PurchaseRequest request,
            User seller) {

        if (request == null) {
            throw new RuntimeException(
                    "Purchase request cannot be null"
            );
        }

        validateSeller(seller);


        /*
         * ---------------------------------------------------------
         * SELLER DISTRIBUTOR RELATIONSHIP
         * ---------------------------------------------------------
         */

        if (request.getSellerDistributorId() == null) {

            throw new RuntimeException(
                    "Seller distributor ID is required"
            );
        }

        SellerDistributor sellerDistributor =
                sellerDistributorRepository
                        .findById(
                                request.getSellerDistributorId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Seller distributor relationship not found"
                                )
                        );


        /*
         * Check seller ownership.
         */

        if (sellerDistributor.getSeller() == null
                || sellerDistributor.getSeller().getId() == null
                || !sellerDistributor.getSeller()
                        .getId()
                        .equals(seller.getId())) {

            throw new RuntimeException(
                    "This distributor is not assigned to this seller"
            );
        }


        /*
         * Check active relationship.
         */

        if (sellerDistributor.getActive() == null
                || !sellerDistributor.getActive()) {

            throw new RuntimeException(
                    "This distributor relationship is inactive"
            );
        }


        /*
         * Check distributor.
         */

        if (sellerDistributor.getDistributor() == null) {

            throw new RuntimeException(
                    "Distributor not found"
            );
        }


        /*
         * ---------------------------------------------------------
         * DISTRIBUTOR BRAND
         * ---------------------------------------------------------
         */

        String distributorBrand =
                normalizeBrand(
                        sellerDistributor.getBrand()
                );

        if (distributorBrand == null) {

            throw new RuntimeException(
                    "Distributor brand is not configured"
            );
        }


        /*
         * ---------------------------------------------------------
         * INVOICE NUMBER
         * ---------------------------------------------------------
         */

        if (request.getInvoiceNumber() == null
                || request.getInvoiceNumber().isBlank()) {

            throw new RuntimeException(
                    "Distributor invoice number is required"
            );
        }

        String invoiceNumber =
                request.getInvoiceNumber().trim();


        if (purchaseRepository
                .existsBySellerAndInvoiceNumber(
                        seller,
                        invoiceNumber
                )) {

            throw new RuntimeException(
                    "This invoice number already exists for this seller"
            );
        }


        /*
         * ---------------------------------------------------------
         * PURCHASE ITEMS
         * ---------------------------------------------------------
         */

        if (request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Purchase must contain at least one product"
            );
        }


        /*
         * ---------------------------------------------------------
         * CREATE PURCHASE
         * ---------------------------------------------------------
         */

        Purchase purchase = new Purchase();

        purchase.setInvoiceNumber(
                invoiceNumber
        );

        /* Preserve the complete distributor tax-invoice header for GST reconciliation. */
        purchase.setIrn(request.getIrn());
        purchase.setAcknowledgementNumber(request.getAcknowledgementNumber());
        purchase.setAcknowledgementDate(request.getAcknowledgementDate());
        purchase.setEwayBillNumber(request.getEwayBillNumber());
        purchase.setModeTermsOfPayment(request.getModeTermsOfPayment());
        purchase.setDeliveryNote(request.getDeliveryNote());
        purchase.setReferenceNumber(request.getReferenceNumber());
        purchase.setOtherReferences(request.getOtherReferences());
        purchase.setBuyerOrderNumber(request.getBuyerOrderNumber());
        purchase.setDispatchDocumentNumber(request.getDispatchDocumentNumber());
        purchase.setDeliveryNoteDate(request.getDeliveryNoteDate());
        purchase.setDispatchedThrough(request.getDispatchedThrough());
        purchase.setDestination(request.getDestination());
        purchase.setTermsOfDelivery(request.getTermsOfDelivery());
        purchase.setEInvoiceQrImageUrl(request.getEInvoiceQrImageUrl());
        purchase.setRoundOff(safeAmount(request.getRoundOff()));

        purchase.setSeller(
                seller
        );

        purchase.setDistributor(
                sellerDistributor
        );

        purchase.setStatus(
                PurchaseStatus.COMPLETED
        );


        if (request.getPurchaseDate() != null) {

            purchase.setPurchaseDate(
                    request.getPurchaseDate()
            );

        } else {

            purchase.setPurchaseDate(
                    LocalDateTime.now()
            );
        }


        purchase.setInvoiceFileUrl(
                request.getInvoiceFileUrl()
        );

        purchase.setNotes(
                request.getNotes()
        );


        /*
         * ---------------------------------------------------------
         * BILL DISCOUNT
         * ---------------------------------------------------------
         */

        BigDecimal billDiscount =
                safeAmount(
                        request.getDiscount()
                );

        if (billDiscount.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new RuntimeException(
                    "Bill discount cannot be negative"
            );
        }

        purchase.setDiscount(
                billDiscount
        );


        /*
         * ---------------------------------------------------------
         * TOTALS
         * ---------------------------------------------------------
         */

        BigDecimal subtotal =
                BigDecimal.ZERO;

        BigDecimal cgst =
                BigDecimal.ZERO;

        BigDecimal sgst =
                BigDecimal.ZERO;

        BigDecimal igst =
                BigDecimal.ZERO;


        /*
         * ---------------------------------------------------------
         * PURCHASE ITEMS
         * ---------------------------------------------------------
         */

        List<PurchaseItem> purchaseItems =
                new ArrayList<>();


        /*
         * Prevent duplicate products.
         */

        Set<String> productSelections =
                new HashSet<>();


        /*
         * Stock changes are stored temporarily.
         */

        List<ProductStockChange> stockChanges =
                new ArrayList<>();


        /*
         * =========================================================
         * LOOP THROUGH ITEMS
         * =========================================================
         */

        for (PurchaseItemRequest itemRequest
                : request.getItems()) {


            if (itemRequest == null) {

                throw new RuntimeException(
                        "Purchase item cannot be null"
                );
            }


            /*
             * -----------------------------------------------------
             * PRODUCT ID
             * -----------------------------------------------------
             */

            if (itemRequest.getProductId() == null) {

                throw new RuntimeException(
                        "Product ID is required"
                );
            }


            /*
             * -----------------------------------------------------
             * DUPLICATE PRODUCT
             * -----------------------------------------------------
             */

            String selectionKey = itemRequest.getProductId() + ":" + (itemRequest.getVariantId() == null ? "base" : itemRequest.getVariantId());
            if (!productSelections.add(selectionKey)) {

                throw new RuntimeException(
                        "Same product cannot be added twice in one purchase: "
                                + itemRequest.getProductId()
                );
            }


            /*
             * -----------------------------------------------------
             * QUANTITY
             * -----------------------------------------------------
             */

            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Product quantity must be greater than zero"
                );
            }


            /*
             * -----------------------------------------------------
             * UNIT PRICE
             * -----------------------------------------------------
             */

            BigDecimal unitPrice =
                    safeAmount(
                            itemRequest.getUnitPrice()
                    );


            if (unitPrice.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                throw new RuntimeException(
                        "Product purchase price must be greater than zero"
                );
            }


            /*
             * -----------------------------------------------------
             * FIND PRODUCT
             * -----------------------------------------------------
             */

            Product product =
                    productRepository
                            .findById(
                                    itemRequest.getProductId()
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Product not found: "
                                                    + itemRequest.getProductId()
                                    )
                            );

            ProductVariant selectedVariant = null;
            if (itemRequest.getVariantId() != null) {
                if (!product.isVariantsEnabled()) throw new RuntimeException("Selected variant is not enabled for this product");
                selectedVariant = productVariantRepository.findByIdAndProduct(itemRequest.getVariantId(), product)
                        .orElseThrow(() -> new RuntimeException("Selected variant does not belong to this product"));
                if (!selectedVariant.isActive()) throw new RuntimeException("Selected variant is inactive");
                if (selectedVariant.getPurchasePrice() != null && unitPrice.signum() <= 0) unitPrice = selectedVariant.getPurchasePrice();
            }


            /*
             * -----------------------------------------------------
             * PRODUCT OWNERSHIP
             * -----------------------------------------------------
             */

            if (product.getSeller() == null
                    || product.getSeller().getId() == null
                    || !product.getSeller()
                            .getId()
                            .equals(seller.getId())) {

                throw new RuntimeException(
                        "Product does not belong to this seller: "
                                + product.getName()
                );
            }


            /*
             * -----------------------------------------------------
             * PRODUCT BRAND
             * -----------------------------------------------------
             */

            String productBrand =
                    normalizeBrand(
                            product.getBrand()
                    );

            /* Older products were saved before Brand existed. Infer the known
             * manufacturer from the product name so an ALL_BRANDS distributor
             * can still be used, then persist the corrected brand. */
            if (productBrand == null) {
                productBrand = inferBrandFromProductName(product.getName());
                if (productBrand != null) {
                    product.setBrand(productBrand);
                    productRepository.save(product);
                }
            }

            Map<String, String> attributes = selectedVariant == null
                    ? validatePurchaseAttributes(product, itemRequest)
                    : productConfigurationService.read(selectedVariant.getAttributesJson());


            if (productBrand == null) {

                throw new RuntimeException(
                        "Product brand is not configured: "
                                + product.getName()
                );
            }


            /*
             * Distributor brand must match product brand.
             */

            if (!"ALL_BRANDS".equalsIgnoreCase(distributorBrand)
                    && !distributorBrand.equalsIgnoreCase(productBrand)) {

                throw new RuntimeException(
                        "Product brand "
                                + productBrand
                                + " does not match distributor brand "
                                + distributorBrand
                );
            }


            /*
             * -----------------------------------------------------
             * ITEM DISCOUNT
             * -----------------------------------------------------
             */

            BigDecimal itemDiscount =
                    safeAmount(
                            itemRequest.getDiscount()
                    );


            if (itemDiscount.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                throw new RuntimeException(
                        "Item discount cannot be negative"
                );
            }


            /*
             * -----------------------------------------------------
             * GST
             * -----------------------------------------------------
             */

            BigDecimal gstRate =
                    safeAmount(
                            itemRequest.getGstRate()
                    );


            if (gstRate.compareTo(
                    BigDecimal.ZERO
            ) < 0
                    || gstRate.compareTo(
                    BigDecimal.valueOf(100)
            ) > 0) {

                throw new RuntimeException(
                        "GST rate must be between 0 and 100"
                );
            }


            /*
             * -----------------------------------------------------
             * GROSS AMOUNT
             * -----------------------------------------------------
             */

            BigDecimal grossAmount =
                    unitPrice
                            .multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.getQuantity()
                                    )
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );


            /*
             * -----------------------------------------------------
             * DISCOUNT VALIDATION
             * -----------------------------------------------------
             */

            if (itemDiscount.compareTo(
                    grossAmount
            ) > 0) {

                throw new RuntimeException(
                        "Item discount cannot be greater than item amount"
                );
            }


            /*
             * -----------------------------------------------------
             * TAXABLE AMOUNT
             * -----------------------------------------------------
             */

            BigDecimal taxableAmount =
                    grossAmount
                            .subtract(
                                    itemDiscount
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );


            /*
             * -----------------------------------------------------
             * CGST / SGST
             * -----------------------------------------------------
             *
             * Assuming intra-state purchase.
             *
             * GST 18%
             *
             * CGST = 9%
             * SGST = 9%
             *
             * -----------------------------------------------------
             */

            BigDecimal cgstRate = itemRequest.getCgstRate() == null
                    ? gstRate.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                    : safeAmount(itemRequest.getCgstRate());

            BigDecimal sgstRate = itemRequest.getSgstRate() == null
                    ? gstRate.subtract(cgstRate)
                    : safeAmount(itemRequest.getSgstRate());

            if (cgstRate.compareTo(BigDecimal.ZERO) < 0
                    || sgstRate.compareTo(BigDecimal.ZERO) < 0
                    || cgstRate.add(sgstRate).compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("CGST and SGST rates must be between 0 and 100");
            }


            BigDecimal itemCgst =
                    taxableAmount
                            .multiply(
                                    cgstRate
                            )
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );


            BigDecimal itemSgst = taxableAmount.multiply(sgstRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);


            BigDecimal itemIgst =
                    BigDecimal.ZERO;


            /*
             * -----------------------------------------------------
             * ITEM TOTAL
             * -----------------------------------------------------
             */

            BigDecimal itemTotal =
                    taxableAmount
                            .add(itemCgst)
                            .add(itemSgst)
                            .add(itemIgst)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );


            /*
             * =====================================================
             * CREATE PURCHASE ITEM
             * =====================================================
             */

            PurchaseItem purchaseItem =
                    new PurchaseItem();


            /*
             * VERY IMPORTANT:
             *
             * Set parent Purchase.
             */

            purchaseItem.setPurchase(
                    purchase
            );


            /*
             * Product.
             */

            purchaseItem.setProduct(
                    product
            );
            purchaseItem.setProductVariantId(selectedVariant == null ? null : selectedVariant.getId());


            purchaseItem.setProductName(
                    product.getName()
            );


            /*
             * SKU.
             */

            purchaseItem.setSku(
                    itemRequest.getSku()
            );
            purchaseItem.setSupplierBarcode(itemRequest.getBarcode());


            /*
             * HSN.
             */

            purchaseItem.setHsnCode(
                    itemRequest.getHsnCode()
            );

            purchaseItem.setColor(itemRequest.getColor());

            /* Keep the invoice specification alongside the item. Product stock
             * and its existing approval workflow remain unchanged. */
            purchaseItem.setBrand(itemRequest.getBrand());
            purchaseItem.setModel(itemRequest.getModel());
            purchaseItem.setRam(itemRequest.getRam());
            purchaseItem.setStorage(itemRequest.getStorage());
            purchaseItem.setDescription(itemRequest.getDescription());
            purchaseItem.setAttributesJson(attributes.isEmpty() ? null : productConfigurationService.write(attributes));


            /*
             * UNIT.
             */

            String unit =
                    itemRequest.getUnit();


            if (unit == null
                    || unit.isBlank()) {

                unit = "NOS";
            }


            purchaseItem.setUnit(
                    unit.trim().toUpperCase()
            );


            /*
             * Quantity.
             */

            purchaseItem.setQuantity(
                    itemRequest.getQuantity()
            );


            /*
             * Price.
             */

            purchaseItem.setUnitPrice(
                    unitPrice
            );


            /*
             * Discount.
             */

            purchaseItem.setDiscount(
                    itemDiscount
            );


            /*
             * GST.
             */

            purchaseItem.setGstRate(
                    gstRate
            );


            /*
             * Taxable amount.
             */

            purchaseItem.setTaxableAmount(
                    taxableAmount
            );


            /*
             * CGST.
             */

            purchaseItem.setCgst(
                    itemCgst
            );


            /*
             * SGST.
             */

            purchaseItem.setSgst(
                    itemSgst
            );


            /*
             * IGST.
             */

            purchaseItem.setIgst(
                    itemIgst
            );


            /*
             * Total.
             */

            purchaseItem.setTotalPrice(
                    itemTotal
            );


            /*
             * IMEI.
             */

            purchaseItem.setImei1(
                    itemRequest.getImei1()
            );


            purchaseItem.setImei2(
                    itemRequest.getImei2()
            );


            /*
             * Serial number.
             */

            purchaseItem.setSerialNumber(
                    itemRequest.getSerialNumber()
            );

            /*
             * Mobile IMEI integrity: quantity 5 means exactly five individual
             * serial rows. Each device needs IMEI 1; IMEI 2 is optional for a
             * single-SIM device, but any provided IMEI is globally unique.
             */
            boolean imeiRequired = Boolean.TRUE.equals(itemRequest.getImeiTrackingRequired());
            purchaseItem.setImeiTrackingRequired(imeiRequired);
            validateSerials(itemRequest, imeiRequired);

            if (itemRequest.getSerials() != null) {
                itemRequest.getSerials().forEach(serialRequest -> {
                    if (serialRequest == null) return;
                    PurchaseItemSerial serial = new PurchaseItemSerial();
                    serial.setImei1(serialRequest.getImei1());
                    serial.setImei2(serialRequest.getImei2());
                    serial.setSerialNumber(serialRequest.getSerialNumber());
                    purchaseItem.addSerial(serial);
                });
            }

            /* Specification changes are reviewed through the existing product
             * approval workflow. Shop inventory is still updated immediately. */
            if (hasCatalogueSpecification(itemRequest)) {
                product.setApprovalStatus(ProductStatus.PENDING);
                product.setActive(false);
                product.setAdminReview(null);
                product.setReviewedAt(null);
            }


            /*
             * Add item to list.
             */

            purchaseItems.add(
                    purchaseItem
            );


            /*
             * -----------------------------------------------------
             * PURCHASE TOTALS
             * -----------------------------------------------------
             */

            subtotal =
                    subtotal.add(
                            taxableAmount
                    );

            cgst =
                    cgst.add(
                            itemCgst
                    );

            sgst =
                    sgst.add(
                            itemSgst
                    );

            igst =
                    igst.add(
                            itemIgst
                    );


            /*
             * =====================================================
             * STOCK
             * =====================================================
             */

            Integer currentStock =
                    product.getStock();


            if (currentStock == null) {
                currentStock = 0;
            }


            long newStock =
                    (long) currentStock
                            + itemRequest.getQuantity();


            if (newStock > Integer.MAX_VALUE) {

                throw new RuntimeException(
                        "Stock value is too large for product: "
                                + product.getName()
                );
            }


            int finalStock =
                    (int) newStock;


            /*
             * Store stock movement information.
             */

            stockChanges.add(
                    new ProductStockChange(
                            product,
                            currentStock,
                            finalStock,
                            itemRequest.getQuantity()
                    )
            );


            /*
             * Update product stock.
             */

            product.setStock(
                    finalStock
            );

            if (selectedVariant != null) {
                long variantStock = (long) selectedVariant.getStockQuantity() + itemRequest.getQuantity();
                if (variantStock > Integer.MAX_VALUE) throw new RuntimeException("Variant stock value is too large");
                selectedVariant.setStockQuantity((int) variantStock);
                productVariantRepository.save(selectedVariant);
            }

            productRepository.save(
                    product
            );
        }


        /*
         * =========================================================
         * BILL DISCOUNT VALIDATION
         * =========================================================
         */

        if (billDiscount.compareTo(
                subtotal
        ) > 0) {

            throw new RuntimeException(
                    "Bill discount cannot be greater than subtotal"
            );
        }


        /*
         * ---------------------------------------------------------
         * FINAL TAXABLE AMOUNT
         * ---------------------------------------------------------
         */

        BigDecimal finalTaxableAmount =
                subtotal
                        .subtract(
                                billDiscount
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        /*
         * ---------------------------------------------------------
         * TOTAL GST
         * ---------------------------------------------------------
         */

        BigDecimal totalGst =
                cgst
                        .add(sgst)
                        .add(igst);


        /*
         * ---------------------------------------------------------
         * GRAND TOTAL
         * ---------------------------------------------------------
         */

        BigDecimal grandTotal =
                finalTaxableAmount
                        .add(totalGst)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );


        /*
         * =========================================================
         * SET PURCHASE TOTALS
         * =========================================================
         */

        purchase.setSubtotal(
                subtotal
        );

        purchase.setCgst(
                cgst
        );

        purchase.setSgst(
                sgst
        );

        purchase.setIgst(
                igst
        );

        purchase.setGrandTotal(
                grandTotal
        );


        /*
         * =========================================================
         * IMPORTANT
         * =========================================================
         *
         * Attach children to parent.
         *
         * purchase.items
         *      ↓
         * purchase_item.purchase_id
         *
         * =========================================================
         */

        purchase.setItems(
                purchaseItems
        );


        /*
         * =========================================================
         * SAVE PURCHASE
         * =========================================================
         *
         * CascadeType.ALL from Purchase entity will save
         * PurchaseItems.
         *
         * =========================================================
         */

        Purchase savedPurchase =
                purchaseRepository.save(
                        purchase
                );


        /*
         * =========================================================
         * FLUSH
         * =========================================================
         *
         * Force Hibernate to execute INSERT statements now.
         *
         * This makes debugging easier and ensures that the
         * purchase and purchase_items are persisted before
         * stock movement creation.
         * =========================================================
         */

        purchaseRepository.flush();


        /*
         * =========================================================
         * CREATE STOCK MOVEMENTS
         * =========================================================
         */

        for (ProductStockChange change
                : stockChanges) {


            StockMovement movement =
                    new StockMovement();


            movement.setProduct(
                    change.product()
            );


            movement.setSeller(
                    seller
            );


            movement.setMovementType(
                    "PURCHASE"
            );


            movement.setQuantity(
                    change.quantity()
            );


            movement.setStockBefore(
                    change.stockBefore()
            );


            movement.setStockAfter(
                    change.stockAfter()
            );


            movement.setReferenceType(
                    "PURCHASE"
            );


            movement.setReferenceId(
                    savedPurchase.getId()
            );


            movement.setNotes(
                    "Stock received from distributor - Invoice: "
                            + savedPurchase.getInvoiceNumber()
            );


            stockMovementRepository.save(
                    movement
            );
        }

        /* Email failure must not undo a successfully saved invoice or stock movement. */
        sendPurchaseInvoiceEmailSafely(savedPurchase, seller);


        /*
         * =========================================================
         * RETURN
         * =========================================================
         */

        return savedPurchase;
    }

    private void validateSerials(PurchaseItemRequest itemRequest, boolean imeiRequired) {
        List<com.shivhub.backend.dto.PurchaseItemSerialRequest> serials = itemRequest.getSerials() == null
                ? List.of() : itemRequest.getSerials();
        if (imeiRequired && serials.size() != itemRequest.getQuantity()) {
            throw new RuntimeException("Mobile IMEI records must exactly match quantity " + itemRequest.getQuantity());
        }
        Set<String> enteredImeis = new HashSet<>();
        for (var serial : serials) {
            if (serial == null) throw new RuntimeException("Invalid IMEI record");
            String imei1 = normalizeImei(serial.getImei1());
            String imei2 = normalizeImei(serial.getImei2());
            if (imeiRequired && imei1 == null) throw new RuntimeException("IMEI 1 is required for every mobile unit");
            validateUniqueImei(imei1, enteredImeis);
            validateUniqueImei(imei2, enteredImeis);
        }
    }

    private void validateUniqueImei(String imei, Set<String> enteredImeis) {
        if (imei == null) return;
        if (!enteredImeis.add(imei)) throw new RuntimeException("Duplicate IMEI entered in this purchase: " + imei);
        if (purchaseItemSerialRepository.existsImeiAnywhere(imei)) throw new RuntimeException("IMEI already exists in ShivHub: " + imei);
    }

    private String normalizeImei(String imei) {
        if (imei == null || imei.isBlank()) return null;
        String clean = imei.trim();
        if (!clean.matches("[0-9]{14,20}")) throw new RuntimeException("IMEI must contain 14 to 20 digits");
        return clean;
    }

    private boolean hasCatalogueSpecification(PurchaseItemRequest item) {
        return hasText(item.getModel()) || hasText(item.getRam())
                || hasText(item.getStorage()) || hasText(item.getDescription());
    }

    private Map<String, String> validatePurchaseAttributes(Product product, PurchaseItemRequest item) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (item.getAttributes() != null) {
            item.getAttributes().forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank())
                    attributes.put(key.trim(), value.trim());
            });
        }
        if (ProductConfigurationService.isMobile(product)) {
            if (!attributes.isEmpty()) throw new RuntimeException("Mobile specifications must use the existing Mobile purchase flow");
            return attributes;
        }
        var fields = productConfigurationService.fields(product);
        productConfigurationService.validateFields(fields, attributes, false);
        Set<String> allowed = new HashSet<>();
        fields.forEach(field -> allowed.add(field.getSpecificationKey()));
        if (!allowed.containsAll(attributes.keySet()))
            throw new RuntimeException("Purchase contains an unconfigured subcategory specification");
        return attributes;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void sendPurchaseInvoiceEmailSafely(Purchase purchase, User seller) {
        try {
            if (seller.getEmail() == null || seller.getEmail().isBlank()) return;
            String distributorName = purchase.getDistributor() != null
                    && purchase.getDistributor().getDistributor() != null
                    ? purchase.getDistributor().getDistributor().getBusinessName() : "Distributor";
            emailService.sendPurchaseCreatedEmail(seller.getEmail(), seller.getName(),
                    purchase.getInvoiceNumber(), distributorName, purchase.getGrandTotal(), purchase.getItems().size());

            /* The approved distributor receives the same invoice acknowledgement when email is available. */
            Distributor distributor = purchase.getDistributor() == null ? null : purchase.getDistributor().getDistributor();
            if (distributor != null && distributor.getEmail() != null && !distributor.getEmail().isBlank()) {
                emailService.sendPurchaseCreatedEmail(distributor.getEmail(), distributor.getContactPerson(),
                        purchase.getInvoiceNumber(), seller.getBusinessName(), purchase.getGrandTotal(), purchase.getItems().size());
            }
        } catch (Exception exception) {
            System.err.println("PURCHASE INVOICE EMAIL FAILED: " + exception.getMessage());
        }
    }


    /*
     * =========================================================
     * GET SELLER PURCHASES
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<Purchase> getSellerPurchases(
            User seller) {

        validateSeller(seller);

        return purchaseRepository
                .findBySellerIdOrderByPurchaseDateDesc(
                        seller.getId()
                );
    }


    /*
     * =========================================================
     * GET PURCHASE BY ID
     * =========================================================
     */

    @Transactional(readOnly = true)
    public Purchase getPurchaseById(
            Long purchaseId,
            User seller) {

        if (purchaseId == null) {

            throw new RuntimeException(
                    "Purchase ID is required"
            );
        }


        validateSeller(seller);


        Purchase purchase =
                purchaseRepository
                        .findById(
                                purchaseId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Purchase not found"
                                )
                        );


        validatePurchaseOwnership(
                purchase,
                seller
        );


        /*
         * Force loading items while transaction is open.
         *
         * This is useful if Purchase.items is LAZY.
         */

        purchase.getItems().size();


        return purchase;
    }


    /*
     * =========================================================
     * GET PURCHASE ITEMS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<PurchaseItem> getPurchaseItems(
            Long purchaseId,
            User seller) {

        Purchase purchase =
                getPurchaseById(
                        purchaseId,
                        seller
                );


        return purchaseItemRepository
                .findByPurchase(
                        purchase
                );
    }


    /*
     * =========================================================
     * GET SELLER PURCHASE COUNT
     * =========================================================
     */

    @Transactional(readOnly = true)
    public long getSellerPurchaseCount(
            User seller) {

        validateSeller(seller);

        return purchaseRepository
                .countBySeller(
                        seller
                );
    }


    /*
     * =========================================================
     * CANCEL PURCHASE
     * =========================================================
     */

    @Transactional
    public Purchase cancelPurchase(
            Long purchaseId,
            User seller) {

        Purchase purchase =
                getPurchaseById(
                        purchaseId,
                        seller
                );


        if (purchase.getStatus()
                == PurchaseStatus.CANCELLED) {

            throw new RuntimeException(
                    "Purchase is already cancelled"
            );
        }


        List<PurchaseItem> items =
                purchaseItemRepository
                        .findByPurchase(
                                purchase
                        );


        if (items == null
                || items.isEmpty()) {

            throw new RuntimeException(
                    "Purchase has no items to reverse"
            );
        }


        /*
         * =====================================================
         * VALIDATE ALL STOCK FIRST
         * =====================================================
         */

        for (PurchaseItem item : items) {


            if (item.getProduct() == null) {

                throw new RuntimeException(
                        "Product not found for purchase item: "
                                + item.getId()
                );
            }


            Product product =
                    item.getProduct();


            Integer currentStock =
                    product.getStock();


            if (currentStock == null) {
                currentStock = 0;
            }


            Integer quantity =
                    item.getQuantity();


            if (quantity == null
                    || quantity <= 0) {

                throw new RuntimeException(
                        "Invalid purchase quantity for item: "
                                + item.getId()
                );
            }


            if (currentStock < quantity) {

                throw new RuntimeException(
                        "Cannot cancel purchase. Current stock "
                                + currentStock
                                + " is less than purchased quantity "
                                + quantity
                                + " for product "
                                + product.getName()
                );
            }
        }


        /*
         * =====================================================
         * REVERSE STOCK
         * =====================================================
         */

        for (PurchaseItem item : items) {


            Product product =
                    item.getProduct();


            int stockBefore =
                    product.getStock();


            int newStock =
                    stockBefore
                            - item.getQuantity();


            product.setStock(
                    newStock
            );


            productRepository.save(
                    product
            );


            /*
             * -------------------------------------------------
             * STOCK MOVEMENT
             * -------------------------------------------------
             */

            StockMovement movement =
                    new StockMovement();


            movement.setProduct(
                    product
            );


            movement.setSeller(
                    seller
            );


            movement.setMovementType(
                    "PURCHASE_CANCEL"
            );


            movement.setQuantity(
                    -item.getQuantity()
            );


            movement.setStockBefore(
                    stockBefore
            );


            movement.setStockAfter(
                    newStock
            );


            movement.setReferenceType(
                    "PURCHASE_CANCEL"
            );


            movement.setReferenceId(
                    purchase.getId()
            );


            movement.setNotes(
                    "Stock reversed because purchase was cancelled - Invoice: "
                            + purchase.getInvoiceNumber()
            );


            stockMovementRepository.save(
                    movement
            );
        }


        /*
         * =====================================================
         * CANCEL PURCHASE
         * =====================================================
         */

        purchase.setStatus(
                PurchaseStatus.CANCELLED
        );


        return purchaseRepository.save(
                purchase
        );
    }


    /*
     * =========================================================
     * VALIDATE SELLER
     * =========================================================
     */

    private void validateSeller(
            User seller) {

        if (seller == null
                || seller.getId() == null) {

            throw new RuntimeException(
                    "Valid seller is required"
            );
        }


        /*
         * Purchases change inventory and must never be created using a
         * customer/admin account that happens to have a valid JWT.
         */
        if (seller.getRole() != Role.SELLER) {

            throw new RuntimeException(
                    "Only seller accounts can manage purchases"
            );
        }
    }


    /*
     * =========================================================
     * VALIDATE PURCHASE OWNERSHIP
     * =========================================================
     */

    private void validatePurchaseOwnership(
            Purchase purchase,
            User seller) {

        if (purchase == null) {

            throw new RuntimeException(
                    "Purchase not found"
            );
        }


        if (purchase.getSeller() == null
                || purchase.getSeller().getId() == null
                || !purchase.getSeller()
                        .getId()
                        .equals(seller.getId())) {

            throw new RuntimeException(
                    "You are not allowed to access this purchase"
            );
        }
    }


    /*
     * =========================================================
     * NORMALIZE BRAND
     * =========================================================
     */

    private String normalizeBrand(
            String brand) {

        if (brand == null
                || brand.isBlank()) {

            return null;
        }

        String value = brand.trim().toUpperCase();

        if (value.contains("REDMI")
                || value.contains("XIAOMI")
                || value.contains("POCO")) {
            return "XIAOMI";
        }

        if (value.contains("PIXEL")
                || value.contains("GOOGLE")) {
            return "GOOGLE PIXEL";
        }

        return value;
    }

    private String inferBrandFromProductName(String productName) {
        if (productName == null) return null;
        String value = productName.trim().toUpperCase();
        if (value.startsWith("IPHONE") || value.startsWith("I PHONE") || value.startsWith("IPAD") || value.startsWith("MACBOOK")) return "APPLE";
        String[] knownBrands = {"SAMSUNG", "APPLE", "VIVO", "OPPO", "REALME", "ONEPLUS", "MOTOROLA", "NOTHING", "GOOGLE", "PIXEL", "XIAOMI", "REDMI", "POCO"};
        for (String brand : knownBrands) {
            if (value.startsWith(brand + " ") || value.equals(brand)) {
                if (brand.equals("GOOGLE") || brand.equals("PIXEL")) return "GOOGLE PIXEL";
                if (brand.equals("REDMI") || brand.equals("POCO")) return "XIAOMI";
                return brand;
            }
        }
        return null;
    }


    /*
     * =========================================================
     * SAFE AMOUNT
     * =========================================================
     */

    private BigDecimal safeAmount(
            BigDecimal amount) {

        if (amount == null) {

            return BigDecimal.ZERO;
        }

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }


    /*
     * =========================================================
     * STOCK CHANGE RECORD
     * =========================================================
     */

    private record ProductStockChange(
            Product product,
            int stockBefore,
            int stockAfter,
            int quantity
    ) {
    }
}
