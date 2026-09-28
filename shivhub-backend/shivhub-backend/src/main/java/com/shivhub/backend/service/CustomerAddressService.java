package com.shivhub.backend.service;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.CustomerAddressRequest;
import com.shivhub.backend.entity.CustomerAddress;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.CustomerAddressRepository;
import com.shivhub.backend.repository.UserRepository;

@Service
public class CustomerAddressService {
    private final CustomerAddressRepository addresses;
    private final UserRepository users;
    public CustomerAddressService(CustomerAddressRepository addresses, UserRepository users) { this.addresses = addresses; this.users = users; }
    private User customer(String email) { User user = users.findByEmail(email).orElseThrow(() -> new RuntimeException("Customer not found")); if (user.getRole() == null || !"CUSTOMER".equals(user.getRole().name())) throw new RuntimeException("Only customers can manage addresses"); return user; }
    @Transactional(readOnly = true) public List<CustomerAddress> list(String email) { return addresses.findByCustomerIdOrderByIsDefaultDescUpdatedAtDesc(customer(email).getId()); }
    @Transactional public CustomerAddress create(String email, CustomerAddressRequest request) { return createForCustomer(customer(email), request); }
    @Transactional public CustomerAddress update(String email, Long id, CustomerAddressRequest request) { User user=customer(email); CustomerAddress address=owned(id,user); copy(request,address); return addresses.save(address); }
    @Transactional public CustomerAddress setDefault(String email, Long id) { User user=customer(email); CustomerAddress address=owned(id,user); addresses.clearDefaultByCustomerId(user.getId()); address.setDefault(true); return addresses.save(address); }
    @Transactional public void delete(String email, Long id) { User user=customer(email); CustomerAddress address=owned(id,user); if (address.isDefault() || addresses.countByCustomerId(user.getId()) <= 1) throw new RuntimeException("Add another address and set it as default before deleting this address"); addresses.delete(address); }
    @Transactional public CustomerAddress resolveForOrder(User customer, Long addressId, CustomerAddressRequest newAddress) { if (addressId != null) return owned(addressId, customer); if (newAddress == null) throw new RuntimeException("A delivery address is required"); return createForCustomer(customer, newAddress); }
    private CustomerAddress createForCustomer(User user, CustomerAddressRequest request) { CustomerAddress address=new CustomerAddress(); address.setCustomerId(user.getId()); copy(request,address); address.setDefault(addresses.countByCustomerId(user.getId()) == 0); return addresses.save(address); }
    private CustomerAddress owned(Long id, User user) { return addresses.findByIdAndCustomerId(id,user.getId()).orElseThrow(() -> new RuntimeException("Address not found")); }
    private void copy(CustomerAddressRequest r, CustomerAddress a) { a.setRecipientName(r.getRecipientName().trim()); a.setMobileNumber(r.getMobileNumber().trim()); a.setAlternateMobileNumber(blank(r.getAlternateMobileNumber())); a.setAddressLine1(r.getAddressLine1().trim()); a.setAddressLine2(r.getAddressLine2().trim()); a.setLandmark(blank(r.getLandmark())); a.setCity(r.getCity().trim()); a.setDistrict(r.getDistrict().trim()); a.setState(r.getState().trim()); a.setPincode(r.getPincode().trim()); a.setAddressLabel(blank(r.getAddressLabel()) == null ? "HOME" : r.getAddressLabel().trim().toUpperCase()); }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
