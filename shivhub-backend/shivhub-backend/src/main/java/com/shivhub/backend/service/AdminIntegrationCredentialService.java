package com.shivhub.backend.service;

import com.shivhub.backend.dto.AdminIntegrationCredentialsResponse;
import com.shivhub.backend.dto.RazorpayCredentialRequest;
import com.shivhub.backend.dto.SmtpCredentialRequest;
import com.shivhub.backend.entity.AdminIntegrationCredential;
import com.shivhub.backend.repository.AdminIntegrationCredentialRepository;
import java.util.Properties;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves SMTP and Razorpay credentials from encrypted admin-maintained data. */
@Service
public class AdminIntegrationCredentialService {
    private static final String SMTP = "SMTP";
    private static final String RAZORPAY = "RAZORPAY";
    private final AdminIntegrationCredentialRepository credentials;
    private final AdminCredentialCipher cipher;
    private final Environment environment;

    public AdminIntegrationCredentialService(AdminIntegrationCredentialRepository credentials,
            AdminCredentialCipher cipher, Environment environment) {
        this.credentials = credentials;
        this.cipher = cipher;
        this.environment = environment;
    }

    public AdminIntegrationCredentialsResponse status() {
        Credential smtp = resolve(SMTP, "spring.mail.username", "spring.mail.password");
        Credential razorpay = resolve(RAZORPAY, "razorpay.key-id", "razorpay.key-secret");
        return new AdminIntegrationCredentialsResponse(cipher.isReady(), smtp.complete(), smtp.source(), smtp.publicValue(),
                razorpay.complete(), razorpay.source(), razorpay.publicValue());
    }

    @Transactional public AdminIntegrationCredentialsResponse saveSmtp(SmtpCredentialRequest request) {
        save(SMTP, request.username(), request.password()); return status();
    }
    @Transactional public AdminIntegrationCredentialsResponse saveRazorpay(RazorpayCredentialRequest request) {
        save(RAZORPAY, request.keyId(), request.keySecret()); return status();
    }
    public Credential smtp() { return resolve(SMTP, "spring.mail.username", "spring.mail.password"); }
    public Credential razorpay() { return resolve(RAZORPAY, "razorpay.key-id", "razorpay.key-secret"); }

    /** Builds a fresh sender when Admin has supplied SMTP credentials. */
    public JavaMailSender mailSender(JavaMailSender fallback) {
        Credential smtp = smtp();
        if (!smtp.complete() || !"ADMIN".equals(smtp.source())) return fallback;
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        if (fallback instanceof JavaMailSenderImpl current) {
            sender.setHost(current.getHost()); sender.setPort(current.getPort()); sender.setProtocol(current.getProtocol());
            sender.setDefaultEncoding(current.getDefaultEncoding());
            Properties currentProperties = current.getJavaMailProperties();
            if (currentProperties != null) sender.getJavaMailProperties().putAll(currentProperties);
        }
        sender.setHost(environment.getProperty("spring.mail.host", sender.getHost() == null ? "smtp.gmail.com" : sender.getHost()));
        sender.setPort(environment.getProperty("spring.mail.port", Integer.class, sender.getPort() > 0 ? sender.getPort() : 587));
        sender.setUsername(smtp.publicValue()); sender.setPassword(smtp.secret());
        copyMailProperty(sender, "mail.smtp.auth"); copyMailProperty(sender, "mail.smtp.starttls.enable");
        copyMailProperty(sender, "mail.smtp.starttls.required"); copyMailProperty(sender, "mail.smtp.connectiontimeout");
        copyMailProperty(sender, "mail.smtp.timeout"); copyMailProperty(sender, "mail.smtp.writetimeout");
        return sender;
    }
    private void copyMailProperty(JavaMailSenderImpl sender, String property) {
        String value = environment.getProperty("spring.mail.properties." + property);
        if (value != null) sender.getJavaMailProperties().setProperty(property, value);
    }
    private void save(String integration, String publicValue, String secret) {
        String value = clean(publicValue), privateValue = clean(secret);
        if (value.isEmpty() || privateValue.isEmpty()) throw new IllegalArgumentException("Both credential values are required");
        AdminIntegrationCredential credential = credentials.findByIntegration(integration).orElseGet(AdminIntegrationCredential::new);
        credential.setIntegration(integration); credential.setPublicValue(value); credential.setEncryptedSecret(cipher.encrypt(privateValue));
        credentials.save(credential);
    }
    private Credential resolve(String integration, String publicProperty, String secretProperty) {
        return credentials.findByIntegration(integration).map(this::fromStored).orElseGet(() -> new Credential(
                clean(environment.getProperty(publicProperty, "")), clean(environment.getProperty(secretProperty, "")), "ENVIRONMENT"));
    }
    private Credential fromStored(AdminIntegrationCredential credential) {
        try { return new Credential(clean(credential.getPublicValue()), clean(cipher.decrypt(credential.getEncryptedSecret())), "ADMIN"); }
        catch (RuntimeException exception) { return new Credential("", "", "ADMIN_UNAVAILABLE"); }
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    public record Credential(String publicValue, String secret, String source) { public boolean complete() { return !publicValue.isBlank() && !secret.isBlank(); } }
}
