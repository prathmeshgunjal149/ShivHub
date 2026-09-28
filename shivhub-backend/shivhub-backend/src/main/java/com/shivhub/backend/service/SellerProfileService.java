package com.shivhub.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.SellerProfileResponse;
import com.shivhub.backend.dto.UpdateSellerProfileRequest;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.UserRepository;

/** Isolates seller profile changes from authentication and customer profile flows. */
@Service
public class SellerProfileService {
    private final UserRepository userRepository;
    public SellerProfileService(UserRepository userRepository) { this.userRepository = userRepository; }
    @Transactional(readOnly = true)
    public SellerProfileResponse get(String email) { return response(seller(email)); }
    @Transactional
    public SellerProfileResponse update(String email, UpdateSellerProfileRequest request) {
        User seller = seller(email);
        if (!seller.getMobile().equals(request.mobile()) && userRepository.existsByMobile(request.mobile()))
            throw new RuntimeException("This mobile number is already in use");
        seller.setName(request.name().trim()); seller.setMobile(request.mobile());
        seller.setBusinessName(blankToNull(request.businessName())); seller.setGstin(blankToNull(request.gstin()));
        seller.setBusinessAddress(blankToNull(request.businessAddress()));
        seller.setLegalBusinessName(blankToNull(request.legalBusinessName())); seller.setShopLogoUrl(blankToNull(request.shopLogoUrl()));
        seller.setAlternateMobile(blankToNull(request.alternateMobile())); seller.setBusinessCity(blankToNull(request.businessCity())); seller.setBusinessDistrict(blankToNull(request.businessDistrict())); seller.setBusinessState(blankToNull(request.businessState())); seller.setBusinessPincode(blankToNull(request.businessPincode())); seller.setGoogleMapsUrl(blankToNull(request.googleMapsUrl())); seller.setWebsiteUrl(blankToNull(request.websiteUrl())); seller.setInstagramUrl(blankToNull(request.instagramUrl())); seller.setFacebookUrl(blankToNull(request.facebookUrl())); seller.setWhatsappUrl(blankToNull(request.whatsappUrl())); seller.setYoutubeUrl(blankToNull(request.youtubeUrl())); seller.setShopOpeningDate(request.shopOpeningDate()); seller.setShopType(blankToNull(request.shopType()));
        seller.setPanNumber(blankToNull(request.panNumber())); seller.setBusinessRegistrationNumber(blankToNull(request.businessRegistrationNumber())); seller.setGstRegistrationType(blankToNull(request.gstRegistrationType())); seller.setStateCode(blankToNull(request.stateCode())); seller.setTaxSettings(blankToNull(request.taxSettings())); seller.setGstRates(blankToNull(request.gstRates())); seller.setHsnSacSettings(blankToNull(request.hsnSacSettings())); seller.setInvoicePrefix(blankToNull(request.invoicePrefix()));
        seller.setInvoiceTerms(blankToNull(request.invoiceTerms()));
        seller.setWarrantyPeriod(blankToNull(request.warrantyPeriod())); seller.setWarrantyType(blankToNull(request.warrantyType())); seller.setReturnPolicy(blankToNull(request.returnPolicy())); seller.setReplacementPolicy(blankToNull(request.replacementPolicy())); seller.setExchangePolicy(blankToNull(request.exchangePolicy())); seller.setRefundPolicy(blankToNull(request.refundPolicy())); seller.setWarrantyTerms(blankToNull(request.warrantyTerms()));
        seller.setInvoiceShowAddress(request.invoiceShowAddress()); seller.setInvoiceShowMobile(request.invoiceShowMobile());
        seller.setInvoiceShowGstin(request.invoiceShowGstin()); seller.setInvoiceShowCustomerDetails(request.invoiceShowCustomerDetails()); seller.setInvoiceShowNotes(request.invoiceShowNotes());
        seller.setInvoiceShowWebsite(request.invoiceShowWebsite()); seller.setInvoiceShowSocialLinks(request.invoiceShowSocialLinks());
        return response(userRepository.save(seller));
    }
    private User seller(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (user.getRole() != Role.SELLER || !user.isEnabled()) throw new RuntimeException("Only active sellers can access profile settings");
        return user;
    }
    private SellerProfileResponse response(User u) { return new SellerProfileResponse(u.getName(),u.getEmail(),u.getMobile(),u.getBusinessName(),u.getLegalBusinessName(),u.getShopLogoUrl(),u.getGstin(),u.getBusinessAddress(),u.getAlternateMobile(),u.getBusinessCity(),u.getBusinessDistrict(),u.getBusinessState(),u.getBusinessPincode(),u.getGoogleMapsUrl(),u.getWebsiteUrl(),u.getInstagramUrl(),u.getFacebookUrl(),u.getWhatsappUrl(),u.getYoutubeUrl(),u.getShopOpeningDate(),u.getShopType(),u.getPanNumber(),u.getBusinessRegistrationNumber(),u.getGstRegistrationType(),u.getStateCode(),u.getTaxSettings(),u.getGstRates(),u.getHsnSacSettings(),u.getInvoicePrefix(),u.getInvoiceTerms(),u.getWarrantyPeriod(),u.getWarrantyType(),u.getReturnPolicy(),u.getReplacementPolicy(),u.getExchangePolicy(),u.getRefundPolicy(),u.getWarrantyTerms(),u.isInvoiceShowAddress(),u.isInvoiceShowMobile(),u.isInvoiceShowGstin(),u.isInvoiceShowCustomerDetails(),u.isInvoiceShowNotes(),u.isInvoiceShowWebsite(),u.isInvoiceShowSocialLinks()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
