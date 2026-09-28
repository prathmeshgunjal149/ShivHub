package com.shivhub.backend.config;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.shivhub.backend.security.JwtAuthenticationFilter;
import com.shivhub.backend.security.SellerSubscriptionAccessFilter;


/*
 * =========================================================
 * SecurityConfig
 * =========================================================
 *
 * Handles:
 *
 * 1. Password encryption
 * 2. JWT authentication
 * 3. CORS
 * 4. Public APIs
 * 5. Admin APIs
 * 6. Seller APIs
 * 7. Customer APIs
 * 8. Stateless sessions
 *
 * =========================================================
 */

@Configuration
public class SecurityConfig {


    /*
     * =====================================================
     * JWT FILTER
     * =====================================================
     */

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SellerSubscriptionAccessFilter sellerSubscriptionAccessFilter;


    private final List<String> corsAllowedOrigins;
    /*
     * =====================================================
     * CONSTRUCTOR
     * =====================================================
     */

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SellerSubscriptionAccessFilter sellerSubscriptionAccessFilter,
            @Value("${shivhub.cors.allowed-origins:http://localhost:5173}")
            String corsAllowedOrigins) {

        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;
        this.sellerSubscriptionAccessFilter = sellerSubscriptionAccessFilter;
        this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }


    /*
     * =====================================================
     * PASSWORD ENCODER
     * =====================================================
     */

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    /*
     * =====================================================
     * CORS CONFIGURATION
     * =====================================================
     */

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        /*
         * React frontend.
         */

        configuration.setAllowedOrigins(corsAllowedOrigins);


        /*
         * Allowed HTTP methods.
         */

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );


        /*
         * Allowed headers.
         */

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );


        /*
         * Exposed headers.
         */

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );


        /*
         * JWT is sent through Authorization header.
         */

        configuration.setAllowCredentials(false);


        /*
         * Apply CORS configuration to all APIs.
         */

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }


    /*
     * =====================================================
     * SECURITY FILTER CHAIN
     * =====================================================
     */

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {


        http


            /*
             * =================================================
             * CORS
             * =================================================
             */

            .cors(cors -> {
            })


            /*
             * =================================================
             * CSRF
             * =================================================
             *
             * JWT REST API.
             */

            .csrf(csrf ->
                    csrf.disable()
            )


            /*
             * =================================================
             * SESSION
             * =================================================
             *
             * JWT is stateless.
             */

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )


            /*
             * =================================================
             * AUTHORIZATION
             * =================================================
             */

            .authorizeHttpRequests(auth -> auth


                /*
                 * =================================================
                 * CORS PREFLIGHT
                 * =================================================
                 */

                .requestMatchers(
                        HttpMethod.OPTIONS,
                        "/**"
                ).permitAll()


                /*
                 * =================================================
                 * AUTH APIs
                 * =================================================
                 *
                 * Login
                 * Register
                 */

                .requestMatchers(
                        "/api/auth/**"
                ).permitAll()

                .requestMatchers(
                        "/api/public/**"
                ).permitAll()

                /* Razorpay signs this raw request; it cannot carry a customer JWT. */
                .requestMatchers(
                        "/api/webhooks/razorpay"
                ).permitAll()


                /*
                 * =================================================
                 * CATEGORY APIs
                 * =================================================
                 *
                 * Public category browsing.
                 */

                .requestMatchers(
                        "/api/categories/**"
                ).permitAll()


                /*
                 * =================================================
                 * PUBLIC PRODUCT LIST
                 * =================================================
                 *
                 * Customer can browse products.
                 *
                 * ProductService itself ensures that only:
                 *
                 * APPROVED + active=true
                 *
                 * products are returned.
                 */

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/products"
                ).permitAll()


                /*
                 * =================================================
                 * PUBLIC PRODUCT DETAILS
                 * =================================================
                 */

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/products/**"
                ).permitAll()


                /*
                 * =================================================
                 * PRODUCT IMAGES
                 * =================================================
                 */

                .requestMatchers(
                        "/uploads/products/**"
                ).permitAll()

                .requestMatchers(
                        "/uploads/campaigns/**"
                ).permitAll()

                /* Approved marketplace photos are displayed in public listing cards. */
                .requestMatchers(
                        "/uploads/marketplace/**"
                ).permitAll()


                /*
                 * =================================================
                 * ADMIN APIs
                 * =================================================
                 *
                 * ONLY ADMIN can access:
                 *
                 * /api/admin/**
                 *
                 * This includes:
                 *
                 * - Pending products
                 * - Approve product
                 * - Reject product
                 * - Seller approvals
                 * - Other admin operations
                 *
                 * =================================================
                 */

                .requestMatchers(
                        "/api/admin/**"
                )
                .hasRole("ADMIN")


                /*
                 * Global distributor catalogue mutations are admin-only.
                 * Sellers must use /api/seller/distributor-requests, which
                 * creates PENDING requests and cannot bypass approval.
                 */
                .requestMatchers(
                        "/api/distributors/**"
                )
                .hasRole("ADMIN")


                /*
                 * =================================================
                 * ALL OTHER APIs
                 * =================================================
                 *
                 * JWT authentication required.
                 */

                .anyRequest().authenticated()
            )


            /*
             * =================================================
             * JWT FILTER
             * =================================================
             */

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            )
            .addFilterAfter(
                    sellerSubscriptionAccessFilter,
                    JwtAuthenticationFilter.class
            );


        /*
         * Build security configuration.
         */

        return http.build();
    }
}
