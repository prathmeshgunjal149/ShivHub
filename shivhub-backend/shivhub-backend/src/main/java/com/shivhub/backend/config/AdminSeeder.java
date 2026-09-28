package com.shivhub.backend.config;

import org.springframework.boot.CommandLineRunner;
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
            PasswordEncoder passwordEncoder) {

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
                            .orElseGet(User::new);

                /*
                 * Create/update the fixed primary admin user.
                 *
                 * Keeping this normalized avoids getting locked out after
                 * local database resets or test-data cleanup.
                 */

                admin.setName("ShivHub Admin");

                admin.setEmail("shivhub007@gmail.com");

                /*
                 * IMPORTANT:
                 *
                 * Password is stored as BCrypt hash.
                 */

                admin.setPassword(
                        passwordEncoder.encode("Admin@123")
                );

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
