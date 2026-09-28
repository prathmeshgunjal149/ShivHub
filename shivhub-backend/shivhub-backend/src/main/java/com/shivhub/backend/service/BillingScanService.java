package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.BillingScanResponse;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.BillingScanType;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/** Resolves exact scanner input against the authenticated seller's physical stock first, then catalogue codes. */
@Service
@RequiredArgsConstructor
public class BillingScanService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PurchaseItemSerialRepository serialRepository;
    @org.springframework.beans.factory.annotation.Autowired private com.shivhub.backend.repository.ProductVariantRepository variants;

    @Transactional(readOnly = true)
    public BillingScanResponse scanProductForBilling(String sellerEmail, String scannedCode) {
        String code = scannedCode == null ? "" : scannedCode.trim();
        if (code.isBlank()) throw new BillingScanException(HttpStatus.BAD_REQUEST, "Please scan or enter a valid code.");
        User seller = seller(sellerEmail);

        List<PurchaseItemSerial> serials = serialRepository.findByExactScanCode(code);
        if (!serials.isEmpty()) return fromSerial(seller, code, serials);
        if (variants != null) {
            var matches = variants.findByProductSellerAndBarcodeIgnoreCase(seller, code);
            if (!matches.isEmpty()) return fromVariant(matches, code, BillingScanType.BARCODE);
        }

        Product barcodeProduct = productRepository.findFirstBySellerAndBarcodeIgnoreCase(seller, code).orElse(null);
        if (barcodeProduct != null) return fromProduct(seller, barcodeProduct, code, BillingScanType.BARCODE);
        if (variants != null) {
            var matches = variants.findByProductSellerAndVariantSkuIgnoreCase(seller, code);
            if (!matches.isEmpty()) return fromVariant(matches, code, BillingScanType.SKU);
        }
        List<Product> skuProducts = productRepository.findBySellerAndSellerSkuIgnoreCase(seller, code);
        if (skuProducts.size() > 1) {
            throw new BillingScanException(HttpStatus.CONFLICT,
                    "More than one product has this SKU. Use the product barcode or correct the duplicate SKU before billing.");
        }
        if (!skuProducts.isEmpty()) return fromProduct(seller, skuProducts.get(0), code, BillingScanType.SKU);
        throw new BillingScanException(HttpStatus.NOT_FOUND, "No product or available IMEI found for this scan.");
    }

    private BillingScanResponse fromSerial(User seller, String code, List<PurchaseItemSerial> matches) {
        PurchaseItemSerial serial = matches.stream().filter(value -> ownedBy(value, seller)).findFirst().orElse(null);
        if (serial == null) throw new BillingScanException(HttpStatus.FORBIDDEN, "Seller is not authorized for this stock.");
        Product product = serial.getPurchaseItem().getProduct();
        if (serial.getPurchaseItem().getPurchase().getStatus() != PurchaseStatus.COMPLETED) {
            throw new BillingScanException(HttpStatus.CONFLICT, "This IMEI is not available for billing.");
        }
        if (serial.isSold() || serial.getSoldOfflineBillItem() != null || serial.getSoldOrderItem() != null) {
            throw new BillingScanException(HttpStatus.CONFLICT, "This IMEI has already been sold.");
        }
        if (serial.isReserved() || serial.getReservedOfflineBillItem() != null || serial.getReservedOrderItem() != null) {
            throw new BillingScanException(HttpStatus.CONFLICT, "This IMEI is reserved for another bill.");
        }
        if (!serial.isAvailable()) throw new BillingScanException(HttpStatus.CONFLICT, "This IMEI is not available for billing.");
        validateSellable(product);
        BillingScanType type = code.equals(serial.getSerialNumber()) ? BillingScanType.SERIAL : BillingScanType.IMEI;
        BillingScanResponse response = base(product, code, type);
        response.setImeiTracked(true);
        response.setSerialId(serial.getId());
        response.setImei1(serial.getImei1());
        response.setImei2(serial.getImei2());
        response.setSerialNumber(serial.getSerialNumber());
        response.setMessage("Mobile found and ready to add to bill");
        return response;
    }

    private BillingScanResponse fromProduct(User seller, Product product, String code, BillingScanType type) {
        validateSellable(product);
        if (product.isVariantsEnabled()) throw new BillingScanException(HttpStatus.CONFLICT,"Select a product variant or scan its individual barcode/SKU.");
        if (requiresExactSerialScan(product)
                || !serialRepository.findAvailableBySellerAndProduct(seller.getId(), product.getId()).isEmpty()) {
            throw new BillingScanException(HttpStatus.CONFLICT,
                    "This product requires an IMEI or serial-number scan before it can be added to a bill.");
        }
        BillingScanResponse response = base(product, code, type);
        response.setImeiTracked(false);
        response.setMessage("Product found and ready to add to bill");
        return response;
    }

    private BillingScanResponse base(Product product, String code, BillingScanType type) {
        BigDecimal gst = product.getGstRate() == null ? BigDecimal.ZERO : product.getGstRate().setScale(2, RoundingMode.HALF_UP);
        BillingScanResponse response = new BillingScanResponse();
        response.setScanType(type); response.setScannedCode(code); response.setProductId(product.getId()); response.setProductName(product.getName());
        response.setBrand(product.getBrand()); response.setModel(product.getModel()); response.setColor(product.getColorOptions()); response.setRam(product.getRam()); response.setStorage(product.getStorage());
        response.setSku(product.getSellerSku()); response.setBarcode(product.getBarcode()); response.setHsnCode(product.getHsnCode()); response.setSellingPrice(product.getFinalSellingPrice());
        response.setGstRate(gst); response.setCgstRate(gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)); response.setSgstRate(gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
        response.setStockAvailable(product.getAvailableStock()); response.setImageUrl(product.getImageUrl()); response.setWarrantyDetails(product.getWarrantyDetails());
        return response;
    }
    private BillingScanResponse fromVariant(List<com.shivhub.backend.entity.ProductVariant> matches, String code, BillingScanType type) {
        if (matches.size() != 1) throw new BillingScanException(HttpStatus.CONFLICT,"Duplicate variant code. Correct the SKU/barcode before billing.");
        var variant=matches.get(0);validateSellable(variant.getProduct());
        if(!variant.isActive()||!variant.getProduct().isVariantsEnabled()||variant.getAvailableStock()<1)throw new BillingScanException(HttpStatus.CONFLICT,"Selected variant is out of stock.");
        BillingScanResponse response=base(variant.getProduct(),code,type);response.setVariantId(variant.getId());response.setSelectedAttributes(variant.getAttributesJson());response.setSellingPrice(variant.getSellingPriceIncludingGst());response.setStockAvailable(variant.getAvailableStock());response.setSku(variant.getVariantSku());response.setBarcode(variant.getBarcode());
        if(variant.getImageUrl()!=null)response.setImageUrl(variant.getImageUrl());response.setMessage("Product variant found and ready to add to bill");return response;
    }

    private void validateSellable(Product product) {
        if (product == null || !product.isActive() || product.getApprovalStatus() != ProductStatus.APPROVED) {
            throw new BillingScanException(HttpStatus.NOT_FOUND, "No product or available IMEI found for this scan.");
        }
        if (product.getAvailableStock() <= 0) throw new BillingScanException(HttpStatus.CONFLICT, "Product is out of stock.");
    }

    private boolean ownedBy(PurchaseItemSerial serial, User seller) {
        return serial.getPurchaseItem() != null && serial.getPurchaseItem().getPurchase() != null
                && serial.getPurchaseItem().getPurchase().getSeller() != null
                && seller.getId().equals(serial.getPurchaseItem().getPurchase().getSeller().getId())
                && serial.getPurchaseItem().getProduct() != null && serial.getPurchaseItem().getProduct().getSeller() != null
                && seller.getId().equals(serial.getPurchaseItem().getProduct().getSeller().getId());
    }

    private boolean requiresExactSerialScan(Product product) {
        if (product.isSerialTrackingRequired()) return true;
        String category = product.getCategoryEntity() == null ? product.getCategory() : product.getCategoryEntity().getName();
        String normalized = category == null ? "" : category.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("mobile") || normalized.contains("phone") || normalized.contains("smartphone");
    }

    private User seller(String email) {
        User seller = userRepository.findByEmail(email).orElseThrow(() -> new BillingScanException(HttpStatus.UNAUTHORIZED, "Seller session was not found."));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) throw new BillingScanException(HttpStatus.FORBIDDEN, "Seller is not authorized for this stock.");
        return seller;
    }
}
