package com.shivhub.backend.controller;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.CustomerAddressRequest;
import com.shivhub.backend.entity.CustomerAddress;
import com.shivhub.backend.service.CustomerAddressService;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/customer/addresses")
public class CustomerAddressController {
    private final CustomerAddressService service; public CustomerAddressController(CustomerAddressService service) { this.service=service; }
    private String email(Authentication auth) { if (auth == null) throw new RuntimeException("Authentication required"); return auth.getName(); }
    @GetMapping public List<CustomerAddress> list(Authentication auth) { return service.list(email(auth)); }
    @PostMapping public ResponseEntity<CustomerAddress> create(@Valid @RequestBody CustomerAddressRequest request, Authentication auth) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(email(auth), request)); }
    @PutMapping("/{addressId}") public CustomerAddress update(@PathVariable Long addressId, @Valid @RequestBody CustomerAddressRequest request, Authentication auth) { return service.update(email(auth), addressId, request); }
    @PatchMapping("/{addressId}/default") public CustomerAddress defaultAddress(@PathVariable Long addressId, Authentication auth) { return service.setDefault(email(auth), addressId); }
    @DeleteMapping("/{addressId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long addressId, Authentication auth) { service.delete(email(auth), addressId); }
}
