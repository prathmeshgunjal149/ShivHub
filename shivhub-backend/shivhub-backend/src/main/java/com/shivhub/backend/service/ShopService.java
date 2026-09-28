package com.shivhub.backend.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.shivhub.backend.dto.CreateShopStaffRequest;
import com.shivhub.backend.dto.ShopRequest;
import com.shivhub.backend.dto.ShopResponse;
import com.shivhub.backend.dto.ShopStaffAssignmentRequest;
import com.shivhub.backend.entity.Shop;
import com.shivhub.backend.entity.ShopStaffAssignment;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.repository.ShopRepository;
import com.shivhub.backend.repository.ShopStaffAssignmentRepository;
import com.shivhub.backend.repository.UserRepository;

/** Business rules for a seller's branch and branch-staff management. */
@Service
public class ShopService {
    private final ShopRepository shopRepository;
    private final ShopStaffAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ShopService(ShopRepository shopRepository, ShopStaffAssignmentRepository assignmentRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.shopRepository = shopRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public ShopResponse createShop(String ownerEmail, ShopRequest request) {
        User owner = verifiedOwner(ownerEmail);
        String code = request.shopCode().trim().toUpperCase();
        if (shopRepository.existsByOwnerAndShopCode(owner, code)) {
            throw new RuntimeException("This shop code already exists for your account");
        }
        Shop shop = new Shop();
        shop.setOwner(owner);
        shop.setShopCode(code);
        copy(request, shop);
        return toResponse(shopRepository.save(shop));
    }

    @Transactional
    public List<ShopResponse> getMyShops(String ownerEmail) {
        User owner = verifiedOwner(ownerEmail);
        List<Shop> shops = shopRepository.findByOwnerOrderByNameAsc(owner);
        if (shops.isEmpty()) {
            Shop primary = new Shop();
            primary.setOwner(owner);
            primary.setShopCode("MAIN-" + owner.getId());
            primary.setName(owner.getBusinessName() == null || owner.getBusinessName().isBlank() ? owner.getName() + " Shop" : owner.getBusinessName());
            primary.setAddress(owner.getBusinessAddress());
            shops = List.of(shopRepository.save(primary));
        }
        return shops.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShopResponse updateShop(String ownerEmail, Long shopId, ShopRequest request) {
        Shop shop = ownedShop(ownerEmail, shopId);
        String code = request.shopCode().trim().toUpperCase();
        if (!code.equals(shop.getShopCode()) && shopRepository.existsByOwnerAndShopCode(shop.getOwner(), code)) {
            throw new RuntimeException("This shop code already exists for your account");
        }
        shop.setShopCode(code);
        copy(request, shop);
        return toResponse(shopRepository.save(shop));
    }

    /** Deactivation preserves invoices and audit history; branches are never hard-deleted. */
    @Transactional
    public void setShopActive(String ownerEmail, Long shopId, boolean active) {
        Shop shop = ownedShop(ownerEmail, shopId);
        shop.setActive(active);
        shopRepository.save(shop);
    }

    @Transactional
    public void assignStaff(String ownerEmail, Long shopId, ShopStaffAssignmentRequest request) {
        Shop shop = ownedShop(ownerEmail, shopId);
        User staff = userRepository.findById(request.staffUserId())
                .orElseThrow(() -> new RuntimeException("Staff user not found"));
        if (staff.getRole() != Role.STAFF || !staff.isEnabled()) {
            throw new RuntimeException("Select an active STAFF account for a shop assignment");
        }
        ShopStaffAssignment assignment = assignmentRepository.findByShopAndStaff(shop, staff)
                .orElseGet(ShopStaffAssignment::new);
        assignment.setShop(shop);
        assignment.setStaff(staff);
        assignment.setAccessRole(request.accessRole());
        assignment.setActive(true);
        assignmentRepository.save(assignment);
    }

    /** Creates a STAFF login, then records its branch-specific permissions. */
    @Transactional
    public void createAndAssignStaff(String ownerEmail, Long shopId, CreateShopStaffRequest request) {
        Shop shop = ownedShop(ownerEmail, shopId);
        if (userRepository.existsByEmail(request.email()) || userRepository.existsByMobile(request.mobile())) {
            throw new RuntimeException("A user with this email or mobile already exists");
        }

        User staff = new User();
        staff.setName(request.name().trim());
        staff.setEmail(request.email().trim().toLowerCase());
        staff.setMobile(request.mobile());
        staff.setPassword(passwordEncoder.encode(request.temporaryPassword()));
        staff.setRole(Role.STAFF);
        staff.setStatus(UserStatus.APPROVED);
        staff.setEnabled(true);
        staff = userRepository.save(staff);

        ShopStaffAssignment assignment = new ShopStaffAssignment();
        assignment.setShop(shop);
        assignment.setStaff(staff);
        assignment.setAccessRole(request.accessRole());
        assignment.setActive(true);
        assignmentRepository.save(assignment);

        try {
            emailService.sendStaffAssignedEmail(staff.getEmail(), staff.getName(), shop.getName(), request.accessRole().name());
        } catch (Exception ignored) {
            // The account remains usable if mail is temporarily unavailable.
        }
    }

    private Shop ownedShop(String ownerEmail, Long shopId) {
        User owner = verifiedOwner(ownerEmail);
        Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new RuntimeException("Shop not found"));
        if (!shop.getOwner().getId().equals(owner.getId())) throw new RuntimeException("You do not own this shop");
        return shop;
    }

    private User verifiedOwner(String ownerEmail) {
        User user = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (user.getRole() != Role.SELLER || !user.isEnabled()) throw new RuntimeException("Only an active seller can manage shops");
        return user;
    }

    private void copy(ShopRequest request, Shop shop) {
        shop.setName(request.name().trim()); shop.setCity(request.city()); shop.setAddress(request.address());
    }

    private ShopResponse toResponse(Shop shop) {
        return new ShopResponse(shop.getId(), shop.getShopCode(), shop.getName(), shop.getCity(), shop.getAddress(), shop.isActive());
    }
}
