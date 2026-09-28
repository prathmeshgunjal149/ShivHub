package com.shivhub.backend.dto;

import java.math.BigDecimal;
import java.util.List;


/*
 * =========================================================
 * CreateProductRequest
 * =========================================================
 *
 * Request received from Seller while creating a product.
 *
 * Contains:
 *
 * 1. Product information
 * 2. Category
 * 3. SubCategory
 * 4. Product images
 *
 * =========================================================
 */

public class CreateProductRequest {
    /** Only consumed by the separate non-mobile creation endpoint. */
    private InitialProductPurchaseRequest initialPurchase;

    public InitialProductPurchaseRequest getInitialPurchase() { return initialPurchase; }
    public void setInitialPurchase(InitialProductPurchaseRequest initialPurchase) { this.initialPurchase = initialPurchase; }


    /*
     * =====================================================
     * PRODUCT INFORMATION
     * =====================================================
     */

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal offerPercentage;

    private Integer stock;

    /* Manufacturer used for distributor-brand validation. */
    private String brand;

    private String model;

    private String modelNumber;

    private String ram;

    private String storage;

    private String colorOptions;

    private String hsnCode;

    private String specificationDetails;

    private String productSpecifications;
    private List<ProductVariantRequest> variants;

    private String shortHighlights;

    private BigDecimal gstRate;

    private String sellerSku;

    private String barcode;

    private Long catalogueProductId;

    private String productType;

    private String physicalCondition;

    private String purchaseTaxTreatment;

    private String saleTaxTreatment;

    private String accessoryType;

    private String compatibility;

    private String warrantyDetails;

    private String packageContents;

    private String taxTreatmentBasis;

    private Integer reorderThreshold;

    private Boolean serialTrackingRequired;


    /*
     * =====================================================
     * CATEGORY
     * =====================================================
     */

    private Long categoryId;

    private Long subCategoryId;


    /*
     * =====================================================
     * PRODUCT IMAGES
     * =====================================================
     *
     * Exactly 5 PRODUCT image URLs are required.
     *
     * Example:
     *
     * [
     *   "image1.jpg",
     *   "image2.jpg",
     *   "image3.jpg",
     *   "image4.jpg",
     *   "image5.jpg"
     * ]
     *
     * =====================================================
     */

    private List<String> images;


    /*
     * =====================================================
     * DEFAULT CONSTRUCTOR
     * =====================================================
     */

    public CreateProductRequest() {
    }


    /*
     * =====================================================
     * GETTERS & SETTERS
     * =====================================================
     */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOfferPercentage() {
        return offerPercentage;
    }

    public void setOfferPercentage(BigDecimal offerPercentage) {
        this.offerPercentage = offerPercentage;
    }


    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getBrand() { return brand; }

    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }

    public void setModel(String model) { this.model = model; }

    public String getModelNumber() { return modelNumber; }

    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public String getRam() { return ram; }

    public void setRam(String ram) { this.ram = ram; }

    public String getStorage() { return storage; }

    public void setStorage(String storage) { this.storage = storage; }

    public String getColorOptions() { return colorOptions; }

    public void setColorOptions(String colorOptions) { this.colorOptions = colorOptions; }

    public String getHsnCode() { return hsnCode; }

    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public String getSpecificationDetails() { return specificationDetails; }

    public void setSpecificationDetails(String specificationDetails) { this.specificationDetails = specificationDetails; }

    public String getProductSpecifications() { return productSpecifications; }

    public void setProductSpecifications(String productSpecifications) { this.productSpecifications = productSpecifications; }
    public List<ProductVariantRequest> getVariants() { return variants; }
    public void setVariants(List<ProductVariantRequest> variants) { this.variants = variants; }

    public String getShortHighlights() { return shortHighlights; }

    public void setShortHighlights(String shortHighlights) { this.shortHighlights = shortHighlights; }

    public BigDecimal getGstRate() { return gstRate; }

    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public String getSellerSku() { return sellerSku; }

    public void setSellerSku(String sellerSku) { this.sellerSku = sellerSku; }

    public String getBarcode() { return barcode; }

    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Long getCatalogueProductId() { return catalogueProductId; }

    public void setCatalogueProductId(Long catalogueProductId) { this.catalogueProductId = catalogueProductId; }

    public String getProductType() { return productType; }

    public void setProductType(String productType) { this.productType = productType; }

    public String getPhysicalCondition() { return physicalCondition; }

    public void setPhysicalCondition(String physicalCondition) { this.physicalCondition = physicalCondition; }

    public String getPurchaseTaxTreatment() { return purchaseTaxTreatment; }

    public void setPurchaseTaxTreatment(String purchaseTaxTreatment) { this.purchaseTaxTreatment = purchaseTaxTreatment; }

    public String getSaleTaxTreatment() { return saleTaxTreatment; }

    public void setSaleTaxTreatment(String saleTaxTreatment) { this.saleTaxTreatment = saleTaxTreatment; }

    public String getAccessoryType() { return accessoryType; }

    public void setAccessoryType(String accessoryType) { this.accessoryType = accessoryType; }

    public String getCompatibility() { return compatibility; }

    public void setCompatibility(String compatibility) { this.compatibility = compatibility; }

    public String getWarrantyDetails() { return warrantyDetails; }

    public void setWarrantyDetails(String warrantyDetails) { this.warrantyDetails = warrantyDetails; }

    public String getPackageContents() { return packageContents; }

    public void setPackageContents(String packageContents) { this.packageContents = packageContents; }

    public String getTaxTreatmentBasis() { return taxTreatmentBasis; }

    public void setTaxTreatmentBasis(String taxTreatmentBasis) { this.taxTreatmentBasis = taxTreatmentBasis; }

    public Integer getReorderThreshold() { return reorderThreshold; }

    public void setReorderThreshold(Integer reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    public Boolean getSerialTrackingRequired() { return serialTrackingRequired; }

    public void setSerialTrackingRequired(Boolean serialTrackingRequired) { this.serialTrackingRequired = serialTrackingRequired; }


    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }


    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(Long subCategoryId) {
        this.subCategoryId = subCategoryId;
    }


    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
