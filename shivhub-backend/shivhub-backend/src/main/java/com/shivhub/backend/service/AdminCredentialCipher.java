package com.shivhub.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Server-side AES-GCM protection for database-stored operational secrets. */
@Service
public class AdminCredentialCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final String masterKey;

    public AdminCredentialCipher(
            @Value("${shivhub.admin-credentials.encryption-key:}") String masterKey) {
        this.masterKey = masterKey == null ? "" : masterKey.trim();
    }

    public boolean isReady() {
        return masterKey.length() >= 32;
    }

    public String encrypt(String plaintext) {
        requireReady();
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return "v1." + encoder.encodeToString(nonce) + "." + encoder.encodeToString(encrypted);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not protect the integration secret", exception);
        }
    }

    public String decrypt(String encoded) {
        requireReady();
        try {
            String[] parts = encoded == null ? new String[0] : encoded.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalStateException("Stored integration secret has an unsupported format");
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] nonce = decoder.decode(parts[1]);
            byte[] encrypted = decoder.decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read the stored integration secret", exception);
        }
    }

    private SecretKeySpec key() throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private void requireReady() {
        if (!isReady()) {
            throw new IllegalStateException("Set SHIVHUB_ADMIN_CREDENTIALS_ENCRYPTION_KEY to a private value of at least 32 characters before saving credentials in Admin");
        }
    }
}
