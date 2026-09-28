package com.shivhub.backend.service;

import com.shivhub.backend.entity.User;
import com.shivhub.backend.entity.WhatsAppOtpVerification;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.WhatsAppOtpVerificationRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration-only WhatsApp OTP workflow. Credentials/codes are never logged. */
@Service
public class WhatsAppOtpService {
    private final UserRepository users; private final WhatsAppOtpVerificationRepository otps;
    private final PasswordEncoder encoder; private final WhatsAppNotificationService whatsapp;
    private final SecureRandom random = new SecureRandom();
    private final int expiryMinutes; private final int resendSeconds; private final int maxAttempts;
    public WhatsAppOtpService(UserRepository users, WhatsAppOtpVerificationRepository otps, PasswordEncoder encoder,
            WhatsAppNotificationService whatsapp,
            @Value("${shivhub.whatsapp.otp-expiry-minutes:5}") int expiryMinutes,
            @Value("${shivhub.whatsapp.otp-resend-seconds:60}") int resendSeconds,
            @Value("${shivhub.whatsapp.otp-max-attempts:5}") int maxAttempts) {
        this.users=users;this.otps=otps;this.encoder=encoder;this.whatsapp=whatsapp;
        this.expiryMinutes=Math.max(1, expiryMinutes);this.resendSeconds=Math.max(30, resendSeconds);this.maxAttempts=Math.max(3,maxAttempts);
    }
    @Transactional public void send(String email) {
        User user = customer(email);
        if (user.isWhatsappVerified()) return;
        WhatsAppOtpVerification previous = otps.findTopByUserIdOrderBySentAtDesc(user.getId()).orElse(null);
        if (previous != null && previous.getSentAt().plusSeconds(resendSeconds).isAfter(LocalDateTime.now()))
            throw new IllegalStateException("Please wait " + resendSeconds + " seconds before requesting another WhatsApp OTP");
        String code = String.format("%06d", random.nextInt(1_000_000));
        if (!whatsapp.sendRegistrationOtp(user, code)) throw new IllegalStateException("WhatsApp OTP could not be sent. Please contact ShivHub support or configure the WhatsApp provider.");
        WhatsAppOtpVerification otp = new WhatsAppOtpVerification(); otp.setUser(user); otp.setOtpHash(encoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes)); otp.setSentAt(LocalDateTime.now()); otps.save(otp);
    }
    @Transactional public void verify(String email, String code) {
        User user=customer(email); WhatsAppOtpVerification otp=otps.findTopByUserIdOrderBySentAtDesc(user.getId()).orElseThrow(()->new IllegalArgumentException("Request a WhatsApp OTP first"));
        if (otp.getVerifiedAt()!=null || LocalDateTime.now().isAfter(otp.getExpiresAt())) throw new IllegalArgumentException("WhatsApp OTP has expired. Request a new code.");
        if (otp.getAttempts() >= maxAttempts) throw new IllegalArgumentException("Too many incorrect OTP attempts. Request a new code.");
        if (!encoder.matches(code, otp.getOtpHash())) { otp.setAttempts(otp.getAttempts()+1);otps.save(otp);throw new IllegalArgumentException("Incorrect WhatsApp OTP"); }
        otp.setVerifiedAt(LocalDateTime.now()); otps.save(otp); user.setWhatsappVerified(true); users.save(user);
    }
    private User customer(String email) { return users.findByEmailIgnoreCase(email == null ? "" : email.trim().toLowerCase())
            .filter(user -> user.getRole()==Role.CUSTOMER).orElseThrow(()->new IllegalArgumentException("Customer account not found")); }
}
