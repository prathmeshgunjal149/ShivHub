package com.shivhub.backend.dto;
import com.shivhub.backend.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Preserves the existing cart JSON shape without serializing seller/private entity fields. */
public record CartLineResponse(Long id,CartProduct product,Integer quantity,Long variantId,String selectedAttributes,
    BigDecimal unitPrice,int availableStock,String variantImageUrl,LocalDateTime createdAt,LocalDateTime updatedAt) {
    public record NamedId(Long id,String name){}
    public record Seller(Long id,String name,String businessName){}
    public record Image(Long id,String imageUrl,String imageType){}
    public record CartProduct(Long id,String name,String description,String brand,String model,String ram,String storage,
        String colorOptions,String imageUrl,BigDecimal price,BigDecimal finalSellingPrice,BigDecimal offerPercentage,
        BigDecimal gstRate,int stock,int availableStock,String category,NamedId categoryEntity,NamedId subCategory,
        Seller seller,List<Image> images,boolean variantsEnabled,String warrantyDetails){}
    public static CartLineResponse from(CartItem line){
        if(line==null)return null;Product p=line.getProduct();User seller=p.getSeller();
        CartProduct product=new CartProduct(p.getId(),p.getName(),p.getDescription(),p.getBrand(),p.getModel(),p.getRam(),p.getStorage(),p.getColorOptions(),p.getImageUrl(),p.getPrice(),p.getFinalSellingPrice(),p.getOfferPercentage(),p.getGstRate(),p.getStock()==null?0:p.getStock(),p.getAvailableStock(),p.getCategory(),p.getCategoryEntity()==null?null:new NamedId(p.getCategoryEntity().getId(),p.getCategoryEntity().getName()),p.getSubCategory()==null?null:new NamedId(p.getSubCategory().getId(),p.getSubCategory().getName()),seller==null?null:new Seller(seller.getId(),seller.getName(),seller.getBusinessName()),p.getImages().stream().map(image->new Image(image.getId(),image.getImageUrl(),String.valueOf(image.getImageType()))).toList(),p.isVariantsEnabled(),p.getWarrantyDetails());
        return new CartLineResponse(line.getId(),product,line.getQuantity(),line.getVariantId(),line.getSelectedAttributes(),line.getUnitPrice(),line.getAvailableStock(),line.getVariantImageUrl(),line.getCreatedAt(),line.getUpdatedAt());
    }
}
