package com.shivhub.backend.service;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class DeliveryEstimateService {
    private final ProductRepository products;private final UserRepository users;private final CustomerAddressRepository addresses;private final DeliveryMapGateway maps;private final DeliveryRuleService rules;
    public DeliveryEstimateService(ProductRepository products,UserRepository users,CustomerAddressRepository addresses,DeliveryMapGateway maps,DeliveryRuleService rules){this.products=products;this.users=users;this.addresses=addresses;this.maps=maps;this.rules=rules;}
    @Transactional public DeliveryEstimateResponse estimate(String email,Long productId,Long addressId){
        User customer=users.findByEmailIgnoreCase(email).filter(u->u.getRole()==com.shivhub.backend.enums.Role.CUSTOMER&&u.isEnabled()).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Customer access required"));
        Product product=products.findById(productId).filter(p->p.isActive()&&p.getApprovalStatus()==com.shivhub.backend.enums.ProductStatus.APPROVED).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));
        return forOrder(customer,product,addressId);
    }
    @Transactional public DeliveryEstimateResponse forOrder(User customer,Product product,Long addressId){
        if(!ProductConfigurationService.isMobile(product))return empty(false,product,"Fast delivery rules apply only to mobiles");
        if(addressId==null)return empty(true,product,"Add/select your delivery address to check delivery time.");
        CustomerAddress address=addresses.findByIdAndCustomerId(addressId,customer.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Delivery address not found"));
        User seller=product.getSeller(); if(seller==null||seller.getBusinessAddress()==null||seller.getBusinessAddress().isBlank())return empty(true,product,"Seller location is unavailable. Delivery will be confirmed by seller.");
        String destination=String.join(", ",address.getAddressLine1(),address.getAddressLine2(),address.getCity(),address.getDistrict(),address.getState(),address.getPincode(),"India");
        String destinationHash=hash(destination),originHash=hash(seller.getBusinessAddress());
        Optional<DeliveryMapGateway.Coordinates> to=destinationHash.equals(address.getGeocodedAddressHash())&&address.getLatitude()!=null&&address.getLongitude()!=null?Optional.of(new DeliveryMapGateway.Coordinates(address.getLatitude().doubleValue(),address.getLongitude().doubleValue())):maps.geocode(destination);
        Optional<DeliveryMapGateway.Coordinates> from=originHash.equals(seller.getBusinessGeocodedAddressHash())&&seller.getBusinessLatitude()!=null&&seller.getBusinessLongitude()!=null?Optional.of(new DeliveryMapGateway.Coordinates(seller.getBusinessLatitude().doubleValue(),seller.getBusinessLongitude().doubleValue())):maps.geocode(seller.getBusinessAddress());
        if(to.isEmpty()||from.isEmpty())return empty(true,product,"Delivery location could not be verified. Delivery time will be confirmed by seller.");
        address.setLatitude(decimal(to.get().latitude()));address.setLongitude(decimal(to.get().longitude()));address.setGeocodedAddressHash(destinationHash);
        seller.setBusinessLatitude(decimal(from.get().latitude()));seller.setBusinessLongitude(decimal(from.get().longitude()));seller.setBusinessGeocodedAddressHash(originHash);
        var road=maps.roadDistance(from.get(),to.get());boolean estimated=road.isEmpty();
        BigDecimal distance=BigDecimal.valueOf(road.orElseGet(()->DeliveryMapGateway.straightLineKm(from.get(),to.get())));
        DeliveryRuleDto rule=rules.match(distance);
        String text=rule==null?"Delivery availability will be confirmed by seller":rule.estimatedDeliveryText();
        return new DeliveryEstimateResponse(true,true,rule==null?null:rule.serviceAvailable(),estimated?"Estimated using straight-line distance, not road distance.":"Estimated using road distance.",text,distance.setScale(2,RoundingMode.HALF_UP),estimated?"HAVERSINE":"ROAD",true,shop(product),seller.getId(),rule==null?null:rule.id(),rule==null?null:rule.estimatedMinutes(),rule==null?BigDecimal.ZERO:rule.deliveryCharge());
    }
    private DeliveryEstimateResponse empty(boolean applicable,Product p,String message){return new DeliveryEstimateResponse(applicable,false,null,message,null,null,null,true,shop(p),p.getSeller()==null?null:p.getSeller().getId(),null,null,BigDecimal.ZERO);}
    private String shop(Product p){return p.getSeller()==null?null:p.getSeller().getBusinessName()==null?p.getSeller().getName():p.getSeller().getBusinessName();}
    private BigDecimal decimal(double v){return BigDecimal.valueOf(v).setScale(7,RoundingMode.HALF_UP);}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
