package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.dto.CreateProductRequest;
import com.shivhub.backend.dto.ProductVariantRequest;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.CategorySpecificationTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductConfigurationService {
    private final CategorySpecificationTemplateRepository templates;
    private final ObjectMapper mapper;
    private final com.shivhub.backend.repository.ProductVariantRepository variants;
    private final com.shivhub.backend.repository.ProductRepository products;
    public ProductConfigurationService(CategorySpecificationTemplateRepository templates, ObjectMapper mapper, com.shivhub.backend.repository.ProductVariantRepository variants, com.shivhub.backend.repository.ProductRepository products) { this.templates=templates; this.mapper=mapper;this.variants=variants;this.products=products; }
    public static boolean isMobile(Product p) { return "Mobiles".equalsIgnoreCase(p.getCategoryEntity()==null?p.getCategory():p.getCategoryEntity().getName()); }

    public void configureNew(Product p, CreateProductRequest request) {
        // Legacy requests are preserved. The dynamic non-mobile page opts into this validation.
        if (!"NON_MOBILE".equals(request.getProductType()) && request.getVariants()==null) return;
        List<CategorySpecificationTemplate> fields=fields(p);
        Map<String,String> values=read(request.getProductSpecifications());
        validateFields(fields,values,false);
        if (request.getVariants()==null || request.getVariants().isEmpty()) return;
        if (request.getVariants().size()>100) fail("A product supports at most 100 variants");
        if (p.isSerialTrackingRequired() && !isMobile(p)) fail("Products with serial tracking must use the existing serial purchase flow");
        Set<String> signatures=new HashSet<>(), barcodes=new HashSet<>(),skus=new HashSet<>();
        for (ProductVariantRequest input:request.getVariants()) {
            ProductVariant variant=new ProductVariant();
            apply(p,variant,input,fields);
            if (!signatures.add(variant.getAttributeSignature())) fail("Duplicate variant combination");
            if(variant.getBarcode()!=null&&(!barcodes.add(variant.getBarcode().toLowerCase(Locale.ROOT))||variants.existsByBarcodeIgnoreCaseAndIdNot(variant.getBarcode(),-1L)||products.findByBarcodeIgnoreCase(variant.getBarcode()).isPresent()||variant.getBarcode().equalsIgnoreCase(p.getBarcode())))fail("Variant barcode is already in use");
            if(variant.getVariantSku()!=null&&(!skus.add(variant.getVariantSku().toLowerCase(Locale.ROOT)) || (p.getSeller()!=null&&p.getSeller().getId()!=null&&variants.existsByVariantSkuIgnoreCaseAndProductSellerIdAndIdNot(variant.getVariantSku(),p.getSeller().getId(),-1L))))fail("Variant SKU is already in use");
            p.getVariants().add(variant);
        }
        p.setVariantsEnabled(true);
        if(p.getVariants().stream().noneMatch(ProductVariant::isActive))fail("At least one variant must be active");
        syncVariantFilterValues(p, p.getVariants());
        p.setStock(p.getVariants().stream().filter(ProductVariant::isActive).mapToInt(ProductVariant::getStockQuantity).sum());
        p.setPrice(p.getVariants().stream().filter(ProductVariant::isActive).map(ProductVariant::getSellingPriceIncludingGst).min(BigDecimal::compareTo).orElse(p.getPrice()));
    }
    public List<CategorySpecificationTemplate> fields(Product p) { return p.getCategoryEntity()==null?List.of():templates.findActiveForCategory(p.getCategoryEntity().getId(),p.getSubCategory()==null?null:p.getSubCategory().getId()); }
    public void apply(Product product, ProductVariant v, ProductVariantRequest r, List<CategorySpecificationTemplate> fields) {
        if (r==null || r.attributes()==null || r.attributes().isEmpty()) fail("Variant attributes are required");
        if(v.getId()!=null&&!Objects.equals(r.version(),v.getVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Variant stock/price changed. Reload the page before saving.");
        Map<String,String> attributes=new TreeMap<>();
        r.attributes().forEach((key,value)->{if(key!=null&&value!=null&&!key.isBlank()&&!value.isBlank())attributes.put(key.trim(),value.trim());});
        validateFields(fields,attributes,true);
        Set<String> allowed=new HashSet<>(); fields.stream().filter(CategorySpecificationTemplate::isVariantEnabled).forEach(field->allowed.add(field.getSpecificationKey()));
        if (allowed.isEmpty()) fail("Configure variant fields for this subcategory first");
        if (!allowed.containsAll(attributes.keySet())) fail("Variant contains an unconfigured attribute");
        if (r.stockQuantity()==null||r.stockQuantity()<0||r.sellingPriceIncludingGst()==null||r.sellingPriceIncludingGst().signum()<=0) fail("Variant price must be positive and stock must be zero or greater");
        if (r.compareAtPrice()!=null&&r.compareAtPrice().compareTo(r.sellingPriceIncludingGst())<0) fail("Compare-at price cannot be lower than selling price");
        if (r.purchasePrice()!=null && r.purchasePrice().signum()<0) fail("Variant purchase price cannot be negative");
        String signature=attributes.entrySet().stream().map(e->e.getKey().toLowerCase(Locale.ROOT)+"="+e.getValue().toLowerCase(Locale.ROOT)).reduce((a,b)->a+"|"+b).orElse("");
        if (signature.length()>500) fail("Variant combination is too long");
        v.setProduct(product); v.setAttributeSignature(signature); v.setAttributesJson(write(attributes));
        v.setVariantSku(clean(r.variantSku())); v.setBarcode(clean(r.barcode()));
        if (v.getVariantSku()!=null&&v.getVariantSku().length()>100||v.getBarcode()!=null&&v.getBarcode().length()>80) fail("Variant SKU or barcode is too long");
        v.setSellingPriceIncludingGst(r.sellingPriceIncludingGst().setScale(2,RoundingMode.HALF_UP)); v.setCompareAtPrice(r.compareAtPrice());
        v.setPurchasePrice(r.purchasePrice()==null?null:r.purchasePrice().setScale(2,RoundingMode.HALF_UP));
        if (r.stockQuantity()<v.getReservedQuantity()) fail("Stock cannot be lower than reserved units");
        v.setStockQuantity(r.stockQuantity()); v.setActive(!Boolean.FALSE.equals(r.active())); v.setReorderThreshold(r.reorderThreshold());
        if (!v.isActive()&&v.getReservedQuantity()>0) fail("A reserved variant cannot be deactivated");
        String image=clean(r.imageUrl()); if(image!=null&&!image.startsWith("/")&&!image.matches("https?://.*")) fail("Variant image must be an upload path or HTTP URL"); v.setImageUrl(image);
    }
    public void validateFields(List<CategorySpecificationTemplate> fields,Map<String,String> values,boolean variant) {
        for (CategorySpecificationTemplate f:fields) {
            if (variant!=f.isVariantEnabled()) continue;
            String value=values.get(f.getSpecificationKey());
            if (value==null||value.isBlank()) { if(f.isRequiredField()||variant) fail(f.getDisplayLabel()+" is required"); continue; }
            if (value.length()>4000) fail(f.getDisplayLabel()+" is too long");
            if ("NUMBER".equals(f.getInputType())) { try{new BigDecimal(value);}catch(NumberFormatException e){fail(f.getDisplayLabel()+" must be a number");} }
            if ("BOOLEAN".equals(f.getInputType())&&!List.of("yes","no","true","false").contains(value.toLowerCase(Locale.ROOT))) fail(f.getDisplayLabel()+" must be Yes or No");
            if (List.of("SELECT","MULTI_SELECT").contains(f.getInputType())) {
                List<String> options=options(f.getOptionsJson());
                for(String selected: "MULTI_SELECT".equals(f.getInputType())?value.split(","):new String[]{value}) if(options.stream().noneMatch(option->option.equalsIgnoreCase(selected.trim()))) fail("Invalid selection for "+f.getDisplayLabel());
            }
        }
    }
    public List<String> options(String raw) { try { return mapper.readValue(raw,new TypeReference<List<String>>(){}); }catch(Exception e){return raw==null?List.of():Arrays.stream(raw.split(",")).map(String::trim).filter(x->!x.isBlank()).toList();} }
    /** Cache only public filter axes; individual serials, prices and seller data are never copied. */
    public void syncVariantFilterValues(Product product, Collection<ProductVariant> productVariants) {
        Set<String> axes = new HashSet<>();
        fields(product).stream().filter(field -> field.isVariantEnabled() && field.isFilterable())
                .forEach(field -> axes.add(field.getSpecificationKey()));
        if (axes.isEmpty()) return;
        Map<String,String> specifications = new LinkedHashMap<>(read(product.getProductSpecifications()));
        Map<String,SortedSet<String>> values = new TreeMap<>();
        for (ProductVariant variant : productVariants) {
            if (!variant.isActive()) continue;
            for (Map.Entry<String,String> entry : read(variant.getAttributesJson()).entrySet()) {
                if (axes.contains(entry.getKey()) && entry.getValue() != null && !entry.getValue().isBlank())
                    values.computeIfAbsent(entry.getKey(), key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(entry.getValue().trim());
            }
        }
        values.forEach((key, selected) -> specifications.put(key, String.join(", ", selected)));
        product.setProductSpecifications(write(specifications));
    }
    public Map<String,String> read(String raw) { if(raw==null||raw.isBlank())return Map.of(); try{return mapper.readValue(raw,new TypeReference<Map<String,String>>(){});}catch(Exception e){fail("Product specifications must be a JSON object");return Map.of();} }
    public String write(Object value) { try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Invalid product attributes",e);} }
    private String clean(String v) { return v==null||v.isBlank()?null:v.trim(); }
    private void fail(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
