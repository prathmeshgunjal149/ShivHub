package com.shivhub.backend.service;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.dto.DeliveryRuleDto;
import com.shivhub.backend.repository.DeliveryDistanceRuleRepository;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DeliveryRuleService {
    private final DeliveryDistanceRuleRepository rules; private final EntityManager em;
    public DeliveryRuleService(DeliveryDistanceRuleRepository rules,EntityManager em){this.rules=rules;this.em=em;}
    @Transactional(readOnly=true) public List<DeliveryRuleDto> list(){return rules.findAllByOrderByMinimumDistanceKmAscPriorityAsc().stream().map(this::dto).toList();}
    @Transactional public void initialize(){
        if(em.find(DeliveryRuleConfiguration.class,1L)==null){em.persist(new DeliveryRuleConfiguration(1L));em.flush();}
        lock(); if(rules.count()!=0)return;
        seed("Local mobile delivery","0","5","Delivery within 30 minutes",30,true);
        seed("Nearby mobile delivery","5","15","Delivery within 1 hour",60,true);
        seed("Regional mobile delivery","15","30","Delivery within 2 hours",120,true);
        seed("Outside fast-delivery area","30",null,"Delivery availability will be confirmed by seller",null,false);
    }
    private void seed(String name,String min,String max,String text,Integer minutes,boolean available){save(null,new DeliveryRuleDto(null,name,"MOBILE_ONLY",new BigDecimal(min),max==null?null:new BigDecimal(max),text,minutes,true,0,BigDecimal.ZERO,available));}
    @Transactional public DeliveryRuleDto save(Long id,DeliveryRuleDto input){
        lock(); validate(input);
        DeliveryDistanceRule rule=id==null?new DeliveryDistanceRule():find(id);
        if(input.active())for(DeliveryDistanceRule other:rules.findByProductScopeAndActiveTrueOrderByPriorityAscMinimumDistanceKmAsc("MOBILE_ONLY")) if(!Objects.equals(other.getId(),id)&&overlaps(input.minimumDistanceKm(),input.maximumDistanceKm(),other.getMinimumDistanceKm(),other.getMaximumDistanceKm()))fail("Active distance ranges cannot overlap");
        rule.setName(input.name().trim());rule.setProductScope("MOBILE_ONLY");rule.setMinimumDistanceKm(input.minimumDistanceKm());rule.setMaximumDistanceKm(input.maximumDistanceKm());rule.setEstimatedDeliveryText(input.estimatedDeliveryText().trim());rule.setEstimatedMinutes(input.estimatedMinutes());rule.setActive(input.active());rule.setPriority(input.priority());rule.setDeliveryCharge(input.deliveryCharge()==null?BigDecimal.ZERO:input.deliveryCharge());rule.setServiceAvailable(input.serviceAvailable());return dto(rules.save(rule));
    }
    @Transactional public DeliveryRuleDto status(Long id,boolean active){DeliveryRuleDto r=dto(find(id));return save(id,new DeliveryRuleDto(r.id(),r.name(),r.productScope(),r.minimumDistanceKm(),r.maximumDistanceKm(),r.estimatedDeliveryText(),r.estimatedMinutes(),active,r.priority(),r.deliveryCharge(),r.serviceAvailable()));}
    @Transactional public void delete(Long id){status(id,false);}
    @Transactional(readOnly=true) public DeliveryRuleDto match(BigDecimal distance){if(distance==null||distance.signum()<0)fail("Distance must be zero or greater");return rules.findByProductScopeAndActiveTrueOrderByPriorityAscMinimumDistanceKmAsc("MOBILE_ONLY").stream().filter(r->distance.compareTo(r.getMinimumDistanceKm())>=0&&(r.getMaximumDistanceKm()==null||distance.compareTo(r.getMaximumDistanceKm())<0)).findFirst().map(this::dto).orElse(null);}
    public static boolean overlaps(BigDecimal min,BigDecimal max,BigDecimal otherMin,BigDecimal otherMax){return (otherMax==null||min.compareTo(otherMax)<0)&&(max==null||otherMin.compareTo(max)<0);}
    public static void validate(DeliveryRuleDto r){
        if(r==null||r.name()==null||r.name().isBlank()||r.estimatedDeliveryText()==null||r.estimatedDeliveryText().isBlank())fail("Rule name and delivery text are required");
        if(r.productScope()!=null&&!"MOBILE_ONLY".equals(r.productScope()))fail("Only MOBILE_ONLY scope is supported");
        if(r.minimumDistanceKm()==null||r.minimumDistanceKm().signum()<0||r.maximumDistanceKm()!=null&&r.maximumDistanceKm().compareTo(r.minimumDistanceKm())<=0)fail("Maximum distance must be greater than minimum distance");
        if(r.serviceAvailable()&&(r.estimatedMinutes()==null||r.estimatedMinutes()<1))fail("Estimated minutes must be positive for available delivery");
        if(r.deliveryCharge()!=null&&r.deliveryCharge().signum()<0)fail("Delivery charge cannot be negative");
    }
    private void lock(){if(em.find(DeliveryRuleConfiguration.class,1L,LockModeType.PESSIMISTIC_WRITE)==null)throw new IllegalStateException("Delivery configuration has not initialized");}
    private DeliveryDistanceRule find(Long id){return rules.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Delivery rule not found"));}
    public DeliveryRuleDto dto(DeliveryDistanceRule r){return new DeliveryRuleDto(r.getId(),r.getName(),r.getProductScope(),r.getMinimumDistanceKm(),r.getMaximumDistanceKm(),r.getEstimatedDeliveryText(),r.getEstimatedMinutes(),r.isActive(),r.getPriority(),r.getDeliveryCharge(),r.isServiceAvailable());}
    private static void fail(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
