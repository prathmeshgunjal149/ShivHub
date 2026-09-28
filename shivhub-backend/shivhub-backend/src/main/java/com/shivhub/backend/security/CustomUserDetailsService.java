package com.shivhub.backend.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.UserRepository;

/*
 * CustomUserDetailsService
 *
 * Spring Security ला database मधून user information
 * मिळवून देण्याचे काम ही class करते.
 *
 * आपल्या ShivHub मध्ये:
 *
 * Email → UserRepository → User → CustomUserDetails
 */

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /*
     * Constructor Injection
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /*
     * Load user using email.
     *
     * Spring Security login वेळी ही method call करेल.
     *
     * Example:
     *
     * user@gmail.com
     *       ↓
     * findByEmail()
     *       ↓
     * users table
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        /*
         * Find user by email.
         *
         * जर user सापडला नाही तर exception throw होईल.
         */
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );

        /*
         * Convert our User entity into
         * Spring Security UserDetails.
         */
        return new CustomUserDetails(user);
    }
}