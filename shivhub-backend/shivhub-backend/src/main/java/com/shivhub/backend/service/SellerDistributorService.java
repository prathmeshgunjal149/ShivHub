package com.shivhub.backend.service;

import com.shivhub.backend.dto.AdminDistributorAssignmentRequest;
import com.shivhub.backend.dto.SellerDistributorRequest;
import com.shivhub.backend.dto.SellerDistributorResponse;
import com.shivhub.backend.entity.Distributor;
import com.shivhub.backend.entity.SellerDistributor;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.DistributorRepository;
import com.shivhub.backend.repository.SellerDistributorRepository;
import com.shivhub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin owns assignments; sellers only read their approved distributor/brand list. */
@Service
@RequiredArgsConstructor
public class SellerDistributorService {

    private final SellerDistributorRepository sellerDistributorRepository;
    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;

    /** Legacy seller endpoint intentionally closed. */
    @Transactional
    public SellerDistributorResponse assignDistributor(String sellerEmail, SellerDistributorRequest request) {
        throw new RuntimeException("Distributor assignment is controlled by ShivHub Admin. Please contact Admin.");
    }

    @Transactional
    public SellerDistributorResponse assignByAdmin(String adminEmail, AdminDistributorAssignmentRequest request) {
        ensureAdmin(adminEmail);
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getRole() != Role.SELLER || !seller.isEnabled()) {
            throw new RuntimeException("Select an active approved seller");
        }

        Distributor distributor = distributorRepository.findById(request.getDistributorId())
                .orElseThrow(() -> new RuntimeException("Distributor not found"));
        if (!"ACTIVE".equalsIgnoreCase(distributor.getStatus())) {
            throw new RuntimeException("This distributor is not approved by ShivHub Admin");
        }

        String brand = normalizeBrand(request.getBrand());
        if (!distributorSupportsBrand(distributor, brand)) {
            throw new RuntimeException("Selected distributor is not configured for brand: " + brand);
        }
        if (sellerDistributorRepository.existsBySellerIdAndDistributorIdAndBrandIgnoreCase(
                seller.getId(), distributor.getId(), brand)) {
            throw new RuntimeException("This distributor is already assigned to this seller for this brand");
        }

        SellerDistributor assignment = new SellerDistributor();
        assignment.setSeller(seller);
        assignment.setDistributor(distributor);
        assignment.setBrand(brand);
        assignment.setAssignedBrands(request.getAssignedBrands() == null || request.getAssignedBrands().isBlank() ? brand : request.getAssignedBrands().trim());
        assignment.setPaymentTerms(blank(request.getPaymentTerms()));
        assignment.setCreditPeriodDays(request.getCreditPeriodDays());
        assignment.setCreditLimit(request.getCreditLimit());
        assignment.setOpeningBalance(request.getOpeningBalance());
        assignment.setOpeningBalanceDate(request.getOpeningBalanceDate());
        assignment.setOpeningBalanceType(blank(request.getOpeningBalanceType()));
        assignment.setPreferredPaymentMethod(blank(request.getPreferredPaymentMethod()));
        assignment.setAssignedSalesperson(blank(request.getAssignedSalesperson()));
        assignment.setSellerNotes(blank(request.getSellerNotes()));
        assignment.setActive(request.getActive() == null || request.getActive());
        return mapToResponse(sellerDistributorRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<SellerDistributorResponse> getAllForAdmin(String adminEmail) {
        ensureAdmin(adminEmail);
        return sellerDistributorRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void removeByAdmin(String adminEmail, Long id) {
        ensureAdmin(adminEmail);
        SellerDistributor assignment = sellerDistributorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Distributor assignment not found"));
        sellerDistributorRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<SellerDistributorResponse> getMyDistributors(String sellerEmail) {
        User seller = getSellerByEmail(sellerEmail);
        return sellerDistributorRepository.findBySellerId(seller.getId()).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SellerDistributorResponse> getMyActiveDistributors(String sellerEmail) {
        User seller = getSellerByEmail(sellerEmail);
        return sellerDistributorRepository.findBySellerIdAndActiveTrue(seller.getId()).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SellerDistributorResponse> getByBrand(String sellerEmail, String brand) {
        User seller = getSellerByEmail(sellerEmail);
        return sellerDistributorRepository.findBySellerAndBrandIgnoreCase(seller, normalizeBrand(brand))
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SellerDistributorResponse> getActiveByBrand(String sellerEmail, String brand) {
        User seller = getSellerByEmail(sellerEmail);
        return sellerDistributorRepository.findBySellerAndBrandIgnoreCaseAndActiveTrue(seller, normalizeBrand(brand))
                .stream().map(this::mapToResponse).toList();
    }

    /** Legacy seller modification endpoints intentionally closed. */
    @Transactional public SellerDistributorResponse updateAssignment(String email, Long id, SellerDistributorRequest request) { throw adminOnly(); }
    @Transactional public SellerDistributorResponse activate(String email, Long id) { throw adminOnly(); }
    @Transactional public SellerDistributorResponse deactivate(String email, Long id) { throw adminOnly(); }
    @Transactional public void deleteAssignment(String email, Long id) { throw adminOnly(); }

    private RuntimeException adminOnly() { return new RuntimeException("Only Admin can change distributor assignments"); }

    private User getSellerByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != Role.SELLER) throw new RuntimeException("Only sellers can view assigned distributors");
        return user;
    }

    private void ensureAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != Role.ADMIN) throw new RuntimeException("Only Admin can assign distributors to sellers");
    }

    private String normalizeBrand(String brand) {
        if (brand == null || brand.isBlank()) throw new RuntimeException("Brand is required");
        String value = brand.trim().toUpperCase();
        if (value.contains("REDMI") || value.contains("XIAOMI") || value.contains("POCO")) return "XIAOMI";
        if (value.contains("PIXEL") || value.contains("GOOGLE")) return "GOOGLE PIXEL";
        return value;
    }

    private boolean distributorSupportsBrand(Distributor distributor, String requestedBrand) {
        String brands = distributor.getBrands();
        if (brands == null || brands.isBlank()) return "ALL_BRANDS".equals(requestedBrand);
        for (String configured : brands.split(",")) {
            String normalized = normalizeBrand(configured);
            if ("ALL_BRANDS".equals(normalized) || normalized.equals(requestedBrand)) return true;
        }
        return false;
    }

    private SellerDistributorResponse mapToResponse(SellerDistributor entity) {
        User seller = entity.getSeller();
        Distributor distributor = entity.getDistributor();
        String sellerName = seller.getName() == null || seller.getName().isBlank() ? seller.getEmail() : seller.getName();
        SellerDistributorResponse response = new SellerDistributorResponse();
        response.setId(entity.getId());
        response.setSellerId(seller.getId());
        response.setSellerName(sellerName);
        response.setSellerEmail(seller.getEmail());
        response.setDistributorId(distributor.getId());
        response.setDistributorName(distributor.getBusinessName());
        response.setDistributorGstin(distributor.getGstin());
        response.setDistributorMobile(distributor.getMobile());
        response.setDistributorEmail(distributor.getEmail());
        response.setDistributorAddress(distributor.getAddress());
        response.setDistributorCity(distributor.getCity());
        response.setDistributorDistrict(distributor.getDistrict());
        response.setDistributorState(distributor.getState());
        response.setDistributorPincode(distributor.getPincode());
        response.setDistributorWhatsapp(distributor.getWhatsappNumber());
        response.setDistributorAlternateMobile(distributor.getAlternateMobile());
        response.setDistributorAccountsEmail(distributor.getAccountsEmail());
        response.setProductCategories(distributor.getProductCategories());
        response.setDistributorType(distributor.getDistributorType());
        response.setSalespersonName(distributor.getSalespersonName());
        response.setSalespersonMobile(distributor.getSalespersonMobile());
        response.setClaimsContact(distributor.getClaimsContact());
        response.setDeliveryTerms(distributor.getDeliveryTerms());
        response.setUsualDeliveryTime(distributor.getUsualDeliveryTime());
        response.setBrand(entity.getBrand());
        response.setAssignedBrands(entity.getAssignedBrands());
        response.setPaymentTerms(entity.getPaymentTerms());
        response.setCreditPeriodDays(entity.getCreditPeriodDays());
        response.setCreditLimit(entity.getCreditLimit());
        response.setOpeningBalance(entity.getOpeningBalance());
        response.setOpeningBalanceDate(entity.getOpeningBalanceDate());
        response.setOpeningBalanceType(entity.getOpeningBalanceType());
        response.setPreferredPaymentMethod(entity.getPreferredPaymentMethod());
        response.setAssignedSalesperson(entity.getAssignedSalesperson());
        response.setSellerNotes(entity.getSellerNotes());
        response.setActive(entity.getActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
