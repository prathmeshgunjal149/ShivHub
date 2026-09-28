package com.shivhub.backend.dto;
import com.shivhub.backend.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.Map;
public record SellerVariantResponse(Long id,String variantSku,String barcode,Map<String,String> attributes,
    BigDecimal sellingPriceIncludingGst,BigDecimal purchasePrice,BigDecimal compareAtPrice,int stockQuantity,int reservedQuantity,
    int availableStock,String imageUrl,boolean active,Integer reorderThreshold,Long version) {
    public static SellerVariantResponse from(ProductVariant v,Map<String,String> attributes){return new SellerVariantResponse(v.getId(),v.getVariantSku(),v.getBarcode(),attributes,v.getSellingPriceIncludingGst(),v.getPurchasePrice(),v.getCompareAtPrice(),v.getStockQuantity(),v.getReservedQuantity(),v.getAvailableStock(),v.getImageUrl(),v.isActive(),v.getReorderThreshold(),v.getVersion());}
}
