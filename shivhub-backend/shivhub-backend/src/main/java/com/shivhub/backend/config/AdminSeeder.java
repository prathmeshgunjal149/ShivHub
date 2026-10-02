package com.shivhub.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.repository.UserRepository;


/*
 * =========================================================
 * AdminSeeder
 * =========================================================
 *
 * Creates the initial ShivHub Admin account.
 *
 * Admin is NOT created through public registration.
 *
 * The initial credential is intentionally not written to application logs.
 *
 * =========================================================
 */

@Configuration
public class AdminSeeder {


    @Bean
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SHIVHUB_ADMIN_BOOTSTRAP_PASSWORD:}") String bootstrapPassword) {

        return args -> {

            /*
             * Check whether an Admin already exists.
             *
             * This prevents creating duplicate Admin
             * every time the application starts.
             */

            User admin =
                    userRepository
                            .findByEmailIgnoreCase("shivhub007@gmail.com")
                            .or(() -> userRepository.findByEmailIgnoreCase("admin@shivhub.com"))
                            .orElse(null);

                /*
                 * Create/update the fixed primary admin user.
                 *
                 * Keeping this normalized avoids getting locked out after
                 * local database resets or test-data cleanup.
                 */

                if (admin == null) {
                    if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
                        return;
                    }
                    admin = new User();
                    admin.setName("ShivHub Admin");
                    admin.setEmail("shivhub007@gmail.com");
                    admin.setPassword(passwordEncoder.encode(bootstrapPassword));
                }

                admin.setEmail("shivhub007@gmail.com");

                /* A populated Railway environment variable is an explicit,
                 * one-time password reset. Leaving it empty preserves the
                 * existing BCrypt password on every normal restart. */
                if (bootstrapPassword != null && !bootstrapPassword.isBlank()) {
                    admin.setPassword(passwordEncoder.encode(bootstrapPassword));
                }

                admin.setRole(Role.ADMIN);

                admin.setStatus(UserStatus.APPROVED);

                admin.setEnabled(true);

                /*
                 * Save Admin.
                 */

                userRepository.save(admin);

        };
    }
}
