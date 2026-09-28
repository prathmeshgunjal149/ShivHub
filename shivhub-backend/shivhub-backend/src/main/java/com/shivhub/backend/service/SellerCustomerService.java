package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.SellerCustomerMapping;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.CustomerProfileRepository;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.SellerCustomerMappingRepository;
import com.shivhub.backend.repository.UserRepository;

/** Seller-scoped POS directory. Online customers are shared; offline profiles stay visible only to their mapped seller. */
@Service
public class SellerCustomerService {
    @org.springframework.beans.factory.annotation.Value("${shivhub.public-api-url:}")
    private String publicApiUrl;
    private final OfflineBillRepository bills;
    private final UserRepository users;
    private final EmailService email;
    private final WhatsAppNotificationService whatsappNotifications;
    private final CustomerProfileRepository profiles;
    private final SellerCustomerMappingRepository mappings;
    private final LoyaltyService loyalty;
    private final CustomerReceivableService receivables;

    public SellerCustomerService(OfflineBillRepository bills, UserRepository users, EmailService email,
            WhatsAppNotificationService whatsappNotifications, CustomerProfileRepository profiles,
            SellerCustomerMappingRepository mappings, LoyaltyService loyalty, CustomerReceivableService receivables) {
        this.bills=bills; this.users=users; this.email=email; this.whatsappNotifications=whatsappNotifications;
        this.profiles=profiles; this.mappings=mappings; this.loyalty=loyalty; this.receivables=receivables;
    }

    @Transactional(readOnly=true)
    public List<SellerCustomerResponse> customers(String sellerEmail) {
        User seller=seller(sellerEmail); Map<String,CustomerSummary> result=new LinkedHashMap<>();
        for(OfflineBill bill:bills.findBySellerIdOrderByCreatedAtDesc(seller.getId())) {
            String mobile=bill.getCustomerMobile(); if(mobile==null||mobile.isBlank()) continue;
            String key=safeNormalize(mobile); if(key.isBlank()) key=mobile.trim();
            final String customerKey=key;
            CustomerSummary item=result.computeIfAbsent(customerKey,ignored->new CustomerSummary(bill.getCustomerName(),bill.getCustomerEmail(),customerKey));
            item.billCount++; item.total=item.total.add(bill.getGrandTotal()==null?BigDecimal.ZERO:bill.getGrandTotal()); if(item.lastVisit==null||bill.getCreatedAt().isAfter(item.lastVisit)) item.lastVisit=bill.getCreatedAt();
        }
        return result.values().stream().map(item->new SellerCustomerResponse(item.name,item.email,item.mobile,item.billCount,item.total,item.lastVisit)).toList();
    }

    @Transactional
    public SellerEmailCampaignResponse sendEmailCampaign(String sellerEmail,SellerEmailCampaignRequest request) {
        User seller=seller(sellerEmail); Set<String> allowed=new HashSet<>();
        for(OfflineBill bill:bills.findBySellerIdOrderByCreatedAtDesc(seller.getId())) if(bill.getCustomerEmail()!=null&&!bill.getCustomerEmail().isBlank()) allowed.add(bill.getCustomerEmail().trim().toLowerCase());
        int sent=0,skipped=0;
        for(String recipient:request.recipientEmails().stream().filter(Objects::nonNull).map(value->value.trim().toLowerCase()).distinct().limit(100).toList()) {
            if(!allowed.contains(recipient)){skipped++;continue;}
            try {
                String senderName=shopName(seller);
                email.sendSellerCustomerCampaignEmail(recipient,senderName,request.subject(),request.message(),request.bannerUrl());
                profiles.findFirstByEmailIgnoreCase(recipient)
                        .filter(profile -> isPermitted(seller, profile) && profile.isWhatsappConsent()
                                && (profile.getOnlineUser() == null || profile.getOnlineUser().isWhatsappOptIn()))
                        .ifPresent(profile -> whatsappNotifications.sendOptedInMobileEvent(
                                profile.getMobile(), WhatsAppNotificationEvent.OFFER_NOTIFICATION,
                                profile.getName(), request.subject(),
                                List.of(senderName, request.subject(), request.message())));
                sent++;
            } catch(Exception ignored){skipped++;}
        }
        return new SellerEmailCampaignResponse(sent,skipped);
    }

    @Transactional(readOnly=true)
    public SellerCustomerLookupResponse lookup(String sellerEmail,String mobile) {
        User seller=seller(sellerEmail); String normalized=CustomerMobileNormalizer.normalizeIndianMobile(mobile); List<String> variants=mobileVariants(normalized);
        List<CustomerProfile> allProfiles=profiles.findByMobileIn(variants);
        List<User> onlineUsers=users.findByMobileIn(variants).stream().filter(user->user.getRole()==Role.CUSTOMER).toList();
        List<CustomerProfile> permitted=allProfiles.stream().filter(profile->isPermitted(seller,profile)).toList();
        CustomerProfile profile=permitted.stream().sorted(Comparator.comparing((CustomerProfile p)->p.getOnlineUser()==null)).findFirst().orElse(null);
        User online=profile!=null?profile.getOnlineUser():onlineUsers.stream().findFirst().orElse(null);
        OfflineBill previous=bills.findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream().filter(bill->normalized.equals(safeNormalize(bill.getCustomerMobile()))).findFirst().orElse(null);
        if(profile==null && online==null && previous==null) return absent(normalized);
        long available=profile==null?0:asLong(loyalty.loyaltyForMobile(normalized,seller.getId()).get("availablePoints"));
        long priorCount=countSellerBills(seller,normalized); BigDecimal due=sellerDue(seller,normalized);
        int duplicates=Math.max(0, permitted.size()+onlineUsers.size()-1);
        if(profile!=null) return new SellerCustomerLookupResponse(true,profile.getOnlineUser()==null?null:profile.getOnlineUser().getId(),profile.getId(),profile.getName(),normalized,profile.getEmail(),profile.getAddress(),profile.getCity(),profile.getDistrict(),profile.getState(),profile.getPincode(),profile.getDateOfBirth(),available,priorCount,due,duplicates);
        if(online!=null) return new SellerCustomerLookupResponse(true,online.getId(),null,online.getName(),normalized,online.getEmail(),previous==null?null:previous.getCustomerAddress(),null,null,null,null,online.getDateOfBirth(),0,priorCount,due,duplicates);
        return new SellerCustomerLookupResponse(true,null,previous.getCustomerProfileId(),previous.getCustomerName(),normalized,previous.getCustomerEmail(),previous.getCustomerAddress(),null,null,null,null,null,0,priorCount,due,0);
    }

    @Transactional
    public SellerWhatsAppOfferResponse sendWhatsAppOffer(String sellerEmail, SellerWhatsAppOfferRequest request) {
        User seller = seller(sellerEmail);
        if (!whatsappNotifications.imageOffersConfigured()) {
            throw new IllegalArgumentException("Ask admin to enable the live AiSensy Offer Image Notification campaign before sending.");
        }
        String imageUrl = offerImageUrl(request.bannerUrl());
        Map<String, CustomerProfile> audience = new LinkedHashMap<>();
        for (SellerCustomerMapping mapping : mappings.findBySeller(seller)) {
            CustomerProfile profile = mapping.getCustomerProfile();
            String mobile = safeNormalize(profile.getMobile());
            if (!mobile.isBlank()) audience.put(mobile, profile);
        }
        // Include billed customers even when their legacy bill has no mapping.
        for (OfflineBill bill : bills.findBySellerIdOrderByCreatedAtDesc(seller.getId())) {
            String mobile = safeNormalize(bill.getCustomerMobile());
            if (mobile.isBlank() || audience.containsKey(mobile)) continue;
            profiles.findByMobileIn(mobileVariants(mobile)).stream()
                    .filter(profile -> isPermitted(seller, profile)).findFirst()
                    .ifPresent(profile -> audience.put(mobile, profile));
        }
        Set<String> targets = request.allCustomers() ? audience.keySet() : new LinkedHashSet<>(
                Optional.ofNullable(request.recipientMobiles()).orElse(List.of()).stream()
                        .map(this::safeNormalize).filter(value -> !value.isBlank()).toList());
        if (targets.isEmpty()) throw new IllegalArgumentException("Select customers or choose All my customers.");
        int accepted = 0, skipped = 0, failed = 0;
        for (String mobile : targets) {
            CustomerProfile profile = audience.get(mobile);
            if (profile == null || !profile.isCommunicationConsent() || !profile.isWhatsappConsent()
                    || (profile.getOnlineUser() != null && (!profile.getOnlineUser().isWhatsappOptIn()
                    || profile.getOnlineUser().isMarketingOptOut()))) { skipped++; continue; }
            boolean sent = whatsappNotifications.sendOptedInMobileImageOffer(mobile, profile.getName(),
                    "seller:" + seller.getId() + ":" + request.subject(),
                    List.of(shopName(seller), request.subject(), request.message()), imageUrl);
            if (sent) accepted++; else failed++;
        }
        return new SellerWhatsAppOfferResponse(accepted, skipped, failed);
    }

    private String offerImageUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (url.startsWith("/uploads/campaigns/")) url = (publicApiUrl == null ? "" : publicApiUrl.replaceAll("/+$", "")) + url;
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
                    || host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0")
                    || host.equals("[::1]") || host.endsWith(".local")) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("WhatsApp images need a public HTTPS image URL. Configure SHIVHUB_PUBLIC_API_URL for uploaded banners.");
        }
        return url;
    }

    @Transactional
    public SellerCustomerLookupResponse registerOrUpdate(String sellerEmail,SellerCustomerUpsertRequest request) {
        User seller=seller(sellerEmail); String normalized=CustomerMobileNormalizer.normalizeIndianMobile(request.getMobile()); List<String> variants=mobileVariants(normalized);
        List<CustomerProfile> candidates=profiles.findByMobileIn(variants);
        CustomerProfile profile=candidates.stream().filter(candidate->isPermitted(seller,candidate)).findFirst().orElse(null);
        User online=users.findByMobileIn(variants).stream().filter(user->user.getRole()==Role.CUSTOMER).findFirst().orElse(null);
        if(profile==null && online!=null) profile=profiles.findByOnlineUser(online).orElse(null);
        if(profile==null && candidates.stream().anyMatch(candidate->candidate.getOnlineUser()==null&&!isPermitted(seller,candidate))) throw new IllegalArgumentException("This mobile belongs to another seller's customer record");
        if(profile==null) { profile=new CustomerProfile(); profile.setMobile(normalized); profile.setCommunicationConsent(online==null||!online.isMarketingOptOut()); if(online!=null){profile.setOnlineUser(online);profile.setName(online.getName());profile.setEmail(online.getEmail());profile.setDateOfBirth(online.getDateOfBirth());} }
        if(request.getDateOfBirth()!=null && (request.getDateOfBirth().isAfter(LocalDate.now()) || request.getDateOfBirth().isBefore(LocalDate.now().minusYears(120)))) throw new IllegalArgumentException("Enter a reasonable date of birth");
        profile.setMobile(normalized); profile.setName(request.getName().trim()); profile.setEmail(blank(request.getEmail())); profile.setAddress(blank(request.getAddress())); profile.setCity(blank(request.getCity())); profile.setDistrict(blank(request.getDistrict())); profile.setState(blank(request.getState())); profile.setPincode(blank(request.getPincode())); profile.setWhatsappConsent(request.isWhatsappConsent()); if(request.getDateOfBirth()!=null) profile.setDateOfBirth(request.getDateOfBirth()); profile=profiles.save(profile);
        SellerCustomerMapping mapping=mappings.findBySellerAndCustomerProfile(seller,profile).orElseGet(SellerCustomerMapping::new); mapping.setSeller(seller); mapping.setCustomerProfile(profile); mappings.save(mapping);
        return lookup(sellerEmail,normalized);
    }

    /** POS-only binding: never trusts a profile ID unless it belongs to this seller/share scope and its mobile matches. */
    @Transactional(readOnly=true)
    public CustomerProfile resolveCustomerProfileForBilling(User seller,Long customerProfileId,String requestMobile) {
        if(customerProfileId==null) return null; String normalized=CustomerMobileNormalizer.normalizeIndianMobile(requestMobile);
        CustomerProfile profile=profiles.findById(customerProfileId).orElseThrow(()->new IllegalArgumentException("Customer profile not found"));
        if(!isPermitted(seller,profile)) throw new SecurityException("Customer profile is not available to this seller");
        if(!normalized.equals(CustomerMobileNormalizer.normalizeIndianMobile(profile.getMobile()))) throw new IllegalArgumentException("Customer profile does not match the supplied mobile number");
        return profile;
    }

    private boolean isPermitted(User seller,CustomerProfile profile){ return seller.getRole()==Role.ADMIN || profile.getOnlineUser()!=null||mappings.findBySellerAndCustomerProfile(seller,profile).isPresent(); }
    private long countSellerBills(User seller,String mobile){ return bills.findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream().filter(bill->mobile.equals(safeNormalize(bill.getCustomerMobile()))).count(); }
    private BigDecimal sellerDue(User seller,String mobile){ return receivables.list(seller).stream().filter(row->mobile.equals(safeNormalize((String)row.get("customerMobile")))).map(row->(BigDecimal)row.get("remainingAmount")).reduce(BigDecimal.ZERO,BigDecimal::add); }
    private String safeNormalize(String mobile){try{return CustomerMobileNormalizer.normalizeIndianMobile(mobile);}catch(Exception ignored){return "";}}
    private List<String> mobileVariants(String mobile){ return List.of(mobile,"+91"+mobile,"91"+mobile); }
    private long asLong(Object value){ return value instanceof Number number?number.longValue():0L; }
    private SellerCustomerLookupResponse absent(String mobile){ return new SellerCustomerLookupResponse(false,null,null,null,mobile,null,null,null,null,null,null,null,0,0,BigDecimal.ZERO,0); }
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private String shopName(User seller){return seller.getBusinessName()==null||seller.getBusinessName().isBlank()?seller.getName():seller.getBusinessName();}
    private User seller(String email){User user=users.findByEmail(email).orElseThrow(()->new RuntimeException("Seller not found"));if(user.getRole()!=Role.SELLER||!user.isEnabled())throw new RuntimeException("Only active sellers can access customers");return user;}
    private static class CustomerSummary { String name,email,mobile;long billCount;BigDecimal total=BigDecimal.ZERO;LocalDateTime lastVisit;CustomerSummary(String name,String email,String mobile){this.name=name==null||name.isBlank()?"Walk-in customer":name;this.email=email;this.mobile=mobile;} }
}
