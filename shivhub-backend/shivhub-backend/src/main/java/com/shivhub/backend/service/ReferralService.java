package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.ReferralResponse;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.*;

@Service
public class ReferralService {
    private static final BigDecimal REWARD_AMOUNT = BigDecimal.valueOf(500);
    private final UserRepository users; private final ReferralRepository referrals; private final CouponRepository coupons; private final ReferralInvitationRepository invitations; private final EmailService email;
    private final SecureRandom random = new SecureRandom();
    public ReferralService(UserRepository users, ReferralRepository referrals, CouponRepository coupons, ReferralInvitationRepository invitations, EmailService email) { this.users=users; this.referrals=referrals; this.coupons=coupons; this.invitations=invitations; this.email=email; }
    @Transactional public void invite(String referrerEmail, String invitedEmail) { if(users.existsByEmail(invitedEmail)) throw new RuntimeException("This email already has a ShivHub account"); String code=nextCouponCode("REF"); ReferralInvitation invite=new ReferralInvitation(); invite.setCode(code); invite.setReferrerCustomerId(users.findByEmail(referrerEmail).orElseThrow(()->new RuntimeException("Customer not found")).getId()); invite.setInvitedEmail(invitedEmail.trim().toLowerCase()); invitations.save(invite); email.sendReferralInvitation(invitedEmail,code); }
    @Transactional public void registerReferral(User customer, String suppliedCode) {
        customer.setReferralCode(nextReferralCode());
        if (suppliedCode == null || suppliedCode.isBlank()) { users.save(customer); return; }
        ReferralInvitation invitation=invitations.findByCodeIgnoreCase(suppliedCode.trim()).filter(i->!i.isUsed()&&i.getInvitedEmail().equalsIgnoreCase(customer.getEmail())).orElseThrow(() -> new RuntimeException("Referral code is invalid for this email or has already been used")); User referrer=users.findById(invitation.getReferrerCustomerId()).orElseThrow(() -> new RuntimeException("Referral code is invalid")); invitation.setUsed(true); invitations.save(invitation);
        if (referrer.getRole() != Role.CUSTOMER) throw new RuntimeException("Referral code is invalid");
        customer.setReferredByCustomerId(referrer.getId());
        users.save(customer);
        Referral referral = new Referral(); referral.setReferrerCustomerId(referrer.getId()); referral.setReferredCustomerId(customer.getId()); referrals.save(referral);
    }
    @Transactional public void rewardForDeliveredOrder(Long customerId, Long orderId) {
        Referral referral = referrals.findByReferredCustomerId(customerId).orElse(null);
        if (referral == null || !"PENDING".equals(referral.getStatus())) return;
        referral.setQualifyingOrderId(orderId); referral.setReferrerCouponCode(createReward(referral.getReferrerCustomerId(), "REFER")); referral.setReferredCouponCode(createReward(referral.getReferredCustomerId(), "WELCOME")); referral.setStatus("REWARDED"); referral.setRewardedAt(java.time.LocalDateTime.now()); referrals.save(referral);
    }
    @Transactional(readOnly = true) public ReferralResponse mine(String email) {
        User customer = users.findByEmail(email).orElseThrow(() -> new RuntimeException("Customer not found"));
        List<ReferralResponse.ReferralItem> items = referrals.findByReferrerCustomerIdOrderByCreatedAtDesc(customer.getId()).stream().map(item -> ReferralResponse.ReferralItem.builder().customerName(users.findById(item.getReferredCustomerId()).map(User::getName).orElse("Customer")).status(item.getStatus()).rewardCouponCode(item.getReferrerCouponCode()).createdAt(item.getCreatedAt()).rewardedAt(item.getRewardedAt()).build()).toList();
        return ReferralResponse.builder().referralCode(customer.getReferralCode()).referralLink("/register?ref=" + customer.getReferralCode()).successfulReferrals(items.stream().filter(item -> "REWARDED".equals(item.getStatus())).count()).referrals(items).build();
    }
    private String createReward(Long customerId, String prefix) { Coupon coupon = new Coupon(); coupon.setCode(nextCouponCode(prefix)); coupon.setTitle("Referral reward"); coupon.setDescription("Your ShivHub referral reward"); coupon.setDiscountType(CouponDiscountType.FIXED); coupon.setDiscountValue(REWARD_AMOUNT); coupon.setMinimumOrderAmount(REWARD_AMOUNT); coupon.setPerCustomerLimit(1); coupon.setTargetCustomerId(customerId); coupon.setActive(true); return coupons.save(coupon).getCode(); }
    private String nextReferralCode() { String code; do { code="SHIV"+Integer.toString(random.nextInt(1679616),36).toUpperCase(); } while(users.existsByReferralCodeIgnoreCase(code)); return code; }
    private String nextCouponCode(String prefix) { String code; do { code=prefix+Integer.toString(random.nextInt(1679616),36).toUpperCase(); } while(coupons.existsByCodeIgnoreCase(code)); return code; }
}
