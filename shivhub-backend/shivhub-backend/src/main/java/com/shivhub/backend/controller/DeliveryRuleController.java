package com.shivhub.backend.controller;
import com.shivhub.backend.dto.DeliveryRuleDto;
import com.shivhub.backend.service.DeliveryRuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/admin/delivery-rules") @PreAuthorize("hasRole('ADMIN')")
public class DeliveryRuleController {
    private final DeliveryRuleService service;
    public DeliveryRuleController(DeliveryRuleService service){this.service=service;}
    @GetMapping public List<DeliveryRuleDto> list(){return service.list();}
    @PostMapping public DeliveryRuleDto create(@Valid @RequestBody DeliveryRuleDto request){return service.save(null,request);}
    @PutMapping("/{id}") public DeliveryRuleDto update(@PathVariable Long id,@Valid @RequestBody DeliveryRuleDto request){return service.save(id,request);}
    @PatchMapping("/{id}/status") public DeliveryRuleDto status(@PathVariable Long id,@RequestBody Status request){return service.status(id,request.active());}
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id){service.delete(id);}
    @PostMapping("/preview") public DeliveryRuleDto preview(@RequestBody Distance request){return service.match(request.distanceKm());}
    public record Status(boolean active){} public record Distance(BigDecimal distanceKm){}
}
