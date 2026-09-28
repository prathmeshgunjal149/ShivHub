package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.dto.ProductVariantRequest;
import com.shivhub.backend.dto.ProductVariantResponse;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductVariant;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.ProductVariantRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class ProductVariantService {
    private final ProductRepository products; private final ProductVariantRepository variants; private final UserRepository users; private final ObjectMapper mapper;
    private final ProductConfigurationService configuration;
    private final jakarta.persistence.EntityManager entityManager;
    public ProductVariantService(ProductRepository products, ProductVariantRepository variants, UserRepository users, ObjectMapper mapper, ProductConfigurationService configuration, jakarta.persistence.EntityManager entityManager){this.products=products;this.variants=variants;this.users=users;this.mapper=mapper;this.configuration=configuration;this.entityManager=entityManager;}
    @Transactional(readOnly=true) public List<ProductVariantResponse> customerVariants(Long productId){ Product p=product(productId); if(!p.isActive()||p.getApprovalStatus()!=ProductStatus.APPROVED) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"); return variants.findByProductAndActiveTrueOrderByIdAsc(p).stream().map(this::response).toList(); }
    public record MobileOption(Long productId,String color,String ram,String storage,BigDecimal price,int availableStock){}
    @Transactional(readOnly=true) public List<MobileOption> mobileOptions(Long id){Product p=product(id);if(!p.isActive()||p.getApprovalStatus()!=ProductStatus.APPROVED)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");if(!ProductConfigurationService.isMobile(p)||p.getSeller()==null||p.getCategoryEntity()==null||p.getBrand()==null||p.getModel()==null)return List.of();return products.findMobileSiblingOptions(p.getSeller().getId(),p.getCategoryEntity().getId(),p.getBrand().toLowerCase(Locale.ROOT),p.getModel().toLowerCase(Locale.ROOT),p.getProductType(),p.getPhysicalCondition(),org.springframework.data.domain.PageRequest.of(0,40)).stream().filter(item->single(item.getColorOptions())&&single(item.getRam())&&single(item.getStorage())).map(item->new MobileOption(item.getId(),item.getColorOptions(),item.getRam(),item.getStorage(),item.getFinalSellingPrice(),item.getAvailableStock())).toList();}
    private boolean single(String value){return value!=null&&!value.isBlank()&&!value.matches(".*[,|/].*");}
    @Transactional(readOnly=true) public List<ProductVariantResponse> sellerVariants(String email,Long productId){ Product p=owned(email,productId); return variants.findByProductOrderByIdAsc(p).stream().map(this::response).toList(); }
    @Transactional(readOnly=true) public List<com.shivhub.backend.dto.SellerVariantResponse> managementVariants(String email,Long productId){Product p=owned(email,productId);return variants.findByProductOrderByIdAsc(p).stream().map(v->com.shivhub.backend.dto.SellerVariantResponse.from(v,read(v.getAttributesJson()))).toList();}
    @Transactional(readOnly=true) public Map<String,Object> configuration(String email,Long productId){Product p=owned(email,productId);Map<String,Object> result=new LinkedHashMap<>();result.put("id",p.getId());result.put("name",p.getName());result.put("categoryId",p.getCategoryEntity()==null?null:p.getCategoryEntity().getId());result.put("subCategoryId",p.getSubCategory()==null?null:p.getSubCategory().getId());result.put("mobile",ProductConfigurationService.isMobile(p));result.put("serialTrackingRequired",p.isSerialTrackingRequired());return result;}
    @Transactional public List<ProductVariantResponse> replaceSellerVariants(String email,Long productId,List<ProductVariantRequest> requests){
        Product product=owned(email,productId); if(requests==null||requests.isEmpty()||requests.size()>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Add between 1 and 100 variants");
        if(product.isSerialTrackingRequired() && !ProductConfigurationService.isMobile(product))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Serial-tracked products use the existing purchase and IMEI flow");
        entityManager.refresh(product,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!product.isVariantsEnabled() && product.getReservedStock() != null && product.getReservedStock() > 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete existing reservations before enabling variants");
        List<ProductVariant> existing=variants.findByProductOrderByIdAsc(product);
        Set<String> signatures=new HashSet<>(), inputBarcodes=new HashSet<>(), inputSkus=new HashSet<>(); Set<ProductVariant> updated=new HashSet<>(); List<ProductVariant> replacement=new ArrayList<>();
        for(ProductVariantRequest request:requests){
            String signature=signature(request.attributes()); if(!signatures.add(signature)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate variant attribute combination");
            ProductVariant v=request.id()==null?existing.stream().filter(item->item.getAttributeSignature().equals(signature)).findFirst().orElseGet(ProductVariant::new):existing.stream().filter(item->item.getId().equals(request.id())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Variant does not belong to product"));
            if(!updated.add(v))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate variant ID");
            if(v.getId()!=null&&!v.getAttributeSignature().equals(signature))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Existing variant combinations cannot be renamed; add a new combination instead");
            configuration.apply(product,v,request,configuration.fields(product));
            if(v.getBarcode()!=null&&!inputBarcodes.add(v.getBarcode().toLowerCase(Locale.ROOT)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate barcode in variant rows");
            if(v.getVariantSku()!=null&&!inputSkus.add(v.getVariantSku().toLowerCase(Locale.ROOT)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate SKU in variant rows");
            Long checkedId=v.getId()==null?-1L:v.getId();
            if(v.getBarcode()!=null&&(variants.existsByBarcodeIgnoreCaseAndIdNot(v.getBarcode(),checkedId)||products.findByBarcodeIgnoreCase(v.getBarcode()).isPresent()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Barcode is already in use");
            if(v.getVariantSku()!=null&&variants.existsByVariantSkuIgnoreCaseAndProductSellerIdAndIdNot(v.getVariantSku(),product.getSeller().getId(),checkedId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Variant SKU is already in use");
            replacement.add(v);
        }
        for(ProductVariant v:existing)if(!updated.contains(v)){if(v.getReservedQuantity()>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reserved variants cannot be removed");v.setActive(false);}
        variants.saveAll(existing); List<ProductVariant> saved=variants.saveAll(replacement);
        product.setVariantsEnabled(true);
        List<ProductVariant> active=variants.findByProductAndActiveTrueOrderByIdAsc(product);
        if(active.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"At least one variant must be active");
        configuration.syncVariantFilterValues(product, active);
        product.setStock(active.stream().mapToInt(ProductVariant::getStockQuantity).sum()); product.setReservedStock(active.stream().mapToInt(ProductVariant::getReservedQuantity).sum()); products.save(product);
        product.setPrice(active.stream().map(ProductVariant::getSellingPriceIncludingGst).min(BigDecimal::compareTo).orElseThrow());
        GstBreakup breakup = new GstCalculator().inclusive(product.getPrice(),product.getGstRate());
        product.setSellingTaxablePrice(breakup.taxableAmount());product.setSellingCgst(breakup.cgst());product.setSellingSgst(breakup.sgst());product.setSellingIgst(breakup.igst());
        return saved.stream().map(this::response).toList();
    }
    @Transactional(readOnly=true) public ProductVariant selectedCustomerVariant(Long productId,Long variantId){ Product p=product(productId); ProductVariant v=variants.findByIdAndProduct(variantId,p).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Variant does not belong to product")); if(!v.isActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Variant is unavailable"); return v; }
    private Product owned(String email,Long id){ User u=users.findByEmailIgnoreCase(email).filter(x->x.getRole()==Role.SELLER).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Seller access is required")); Product p=product(id); if(p.getSeller()==null||!Objects.equals(p.getSeller().getId(),u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot change another seller's product"); return p; }
    private Product product(Long id){return products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));}
    private Map<String,String> normalize(Map<String,String> values){if(values==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Variant attributes are required");Map<String,String> result=new TreeMap<>(); values.forEach((k,v)->{if(k!=null&&v!=null&&!k.isBlank()&&!v.isBlank())result.put(k.trim().toLowerCase(Locale.ROOT),v.trim());});if(result.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Variant attributes are required");return result;}
    private String signature(Map<String,String> values){return normalize(values).entrySet().stream().map(e->e.getKey()+"="+e.getValue().toLowerCase(Locale.ROOT)).reduce((a,b)->a+"|"+b).orElseThrow();}
    private String write(Map<String,String> value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid variant attributes");}}
    private Map<String,String> read(String raw){try{return mapper.readValue(raw,new TypeReference<Map<String,String>>(){});}catch(Exception e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Stored variant attributes could not be read");}}
    private ProductVariantResponse response(ProductVariant v){return new ProductVariantResponse(v.getId(),v.getVariantSku(),read(v.getAttributesJson()),v.getSellingPriceIncludingGst(),v.getPurchasePrice(),v.getCompareAtPrice(),v.getAvailableStock(),v.getImageUrl(),v.isActive());}
    private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
