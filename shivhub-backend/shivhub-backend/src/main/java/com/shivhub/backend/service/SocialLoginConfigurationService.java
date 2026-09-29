package com.shivhub.backend.service;

import com.shivhub.backend.dto.AdminSocialLoginConfigurationResponse;
import com.shivhub.backend.dto.PublicSocialLoginConfigurationResponse;
import com.shivhub.backend.dto.SocialLoginProviderRequest;
import com.shivhub.backend.entity.SocialLoginConfiguration;
import com.shivhub.backend.repository.SocialLoginConfigurationRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves OAuth browser identifiers from Admin settings, with environment fallback for deployment. */
@Service
public class SocialLoginConfigurationService {
    private static final long SINGLETON_ID = 1L;
    private final SocialLoginConfigurationRepository settings;
    private final Environment environment;

    public SocialLoginConfigurationService(SocialLoginConfigurationRepository settings, Environment environment) {
        this.settings = settings;
        this.environment = environment;
    }

    public String googleClientId() {
        String stored = storedGoogleClientId();
        return stored.isBlank() ? clean(environment.getProperty("shivhub.oauth.google.client-id", "")) : stored;
    }

    public String facebookAppId() {
        String stored = storedFacebookAppId();
        return stored.isBlank() ? clean(environment.getProperty("shivhub.oauth.facebook.app-id", "")) : stored;
    }

    public PublicSocialLoginConfigurationResponse publicConfiguration() {
        return new PublicSocialLoginConfigurationResponse(googleClientId(), facebookAppId());
    }

    public AdminSocialLoginConfigurationResponse adminConfiguration() {
        String googleStored = storedGoogleClientId();
        String facebookStored = storedFacebookAppId();
        String google = googleStored.isBlank() ? clean(environment.getProperty("shivhub.oauth.google.client-id", "")) : googleStored;
        String facebook = facebookStored.isBlank() ? clean(environment.getProperty("shivhub.oauth.facebook.app-id", "")) : facebookStored;
        return new AdminSocialLoginConfigurationResponse(!google.isBlank(), google,
                googleStored.isBlank() ? "ENVIRONMENT" : "ADMIN", !facebook.isBlank(), facebook,
                facebookStored.isBlank() ? "ENVIRONMENT" : "ADMIN");
    }

    @Transactional
    public AdminSocialLoginConfigurationResponse saveGoogle(SocialLoginProviderRequest request) {
        SocialLoginConfiguration configuration = editable();
        configuration.setGoogleClientId(clean(request.clientId()));
        settings.save(configuration);
        return adminConfiguration();
    }

    @Transactional
    public AdminSocialLoginConfigurationResponse saveFacebook(SocialLoginProviderRequest request) {
        SocialLoginConfiguration configuration = editable();
        configuration.setFacebookAppId(clean(request.clientId()));
        settings.save(configuration);
        return adminConfiguration();
    }

    private SocialLoginConfiguration editable() {
        SocialLoginConfiguration configuration = settings.findById(SINGLETON_ID)
                .orElseGet(SocialLoginConfiguration::new);
        configuration.setId(SINGLETON_ID);
        return configuration;
    }

    private String storedGoogleClientId() {
        return settings.findById(SINGLETON_ID).map(SocialLoginConfiguration::getGoogleClientId).map(this::clean).orElse("");
    }

    private String storedFacebookAppId() {
        return settings.findById(SINGLETON_ID).map(SocialLoginConfiguration::getFacebookAppId).map(this::clean).orElse("");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
