package com.shivhub.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/*
 * =========================================================
 * JwtAuthenticationFilter
 * =========================================================
 *
 * This filter checks JWT authentication for protected APIs.
 *
 * Flow:
 *
 * Request
 *    ↓
 * Authorization Header
 *    ↓
 * Bearer JWT
 *    ↓
 * Extract Email
 *    ↓
 * Load User From Database
 *    ↓
 * Validate JWT
 *    ↓
 * Create Spring Security Authentication
 *    ↓
 * SecurityContext
 *
 * =========================================================
 */

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {


    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;

        this.userDetailsService =
                userDetailsService;
    }


    /*
     * =========================================================
     * PUBLIC AUTH APIs
     * =========================================================
     *
     * These APIs do not require JWT:
     *
     * /api/auth/login
     * /api/auth/register
     * /api/auth/register/seller
     *
     * Therefore the JWT filter will skip them.
     *
     * =========================================================
     */

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        String path =
                request.getServletPath();


        return path.startsWith(
                "/api/auth/"
        );
    }


    /*
     * =========================================================
     * JWT FILTER
     * =========================================================
     */

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {


        /*
         * =====================================================
         * REQUEST INFORMATION
         * =====================================================
         */

        System.out.println();
        System.out.println(
                "================================================="
        );

        System.out.println(
                "JWT FILTER REQUEST : " +
                request.getMethod() +
                " " +
                request.getRequestURI()
        );


        /*
         * =====================================================
         * READ AUTHORIZATION HEADER
         * =====================================================
         */

        String authorizationHeader =
                request.getHeader(
                        "Authorization"
                );


        /*
         * =====================================================
         * CHECK TOKEN
         * =====================================================
         */

        if (authorizationHeader == null
                || !authorizationHeader
                        .startsWith("Bearer ")) {


            System.out.println(
                    "JWT TOKEN : NOT FOUND"
            );


            /*
             * No JWT.
             *
             * Continue request.
             *
             * Spring Security will later decide
             * whether authentication is required.
             */

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * =====================================================
         * EXTRACT TOKEN
         * =====================================================
         */

        String token =
                authorizationHeader.substring(7);


        System.out.println(
                "JWT TOKEN : FOUND"
        );


        try {


            /*
             * =================================================
             * EXTRACT EMAIL
             * =================================================
             */

            String email =
                    jwtService.extractEmail(
                            token
                    );


            System.out.println(
                    "JWT EMAIL : " +
                    email
            );


            /*
             * =================================================
             * CHECK EXISTING AUTHENTICATION
             * =================================================
             */

            if (email != null
                    && SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            == null) {


                /*
                 * =============================================
                 * LOAD USER FROM DATABASE
                 * =============================================
                 */

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        email
                                );


                /*
                 * =============================================
                 * DEBUG USER INFORMATION
                 * =============================================
                 *
                 * IMPORTANT:
                 *
                 * We want to verify that ADMIN becomes:
                 *
                 * ROLE_ADMIN
                 *
                 * =============================================
                 */

                System.out.println(
                        "-----------------------------------------"
                );

                System.out.println(
                        "JWT USER EMAIL : " +
                        userDetails.getUsername()
                );

                System.out.println(
                        "JWT USER AUTHORITIES : " +
                        userDetails.getAuthorities()
                );

                System.out.println(
                        "JWT USER ENABLED : " +
                        userDetails.isEnabled()
                );

                System.out.println(
                        "-----------------------------------------"
                );


                /*
                 * =============================================
                 * VALIDATE JWT
                 * =============================================
                 */

                boolean tokenValid =
                        jwtService.isTokenValid(
                                token,
                                userDetails.getUsername()
                        );


                System.out.println(
                        "JWT TOKEN VALID : " +
                        tokenValid
                );


                /*
                 * =============================================
                 * CREATE AUTHENTICATION
                 * =============================================
                 */

                if (tokenValid) {


                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );


                    /*
                     * =========================================
                     * REQUEST DETAILS
                     * =========================================
                     */

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(
                                            request
                                    )
                    );


                    /*
                     * =========================================
                     * SET SECURITY CONTEXT
                     * =========================================
                     */

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );


                    /*
                     * =========================================
                     * SUCCESS DEBUG
                     * =========================================
                     */

                    System.out.println(
                            "SPRING SECURITY AUTHENTICATION : SUCCESS"
                    );

                    System.out.println(
                            "AUTHENTICATED USER : " +
                            userDetails.getUsername()
                    );

                    System.out.println(
                            "AUTHENTICATED AUTHORITIES : " +
                            userDetails.getAuthorities()
                    );


                } else {


                    /*
                     * =========================================
                     * INVALID TOKEN
                     * =========================================
                     */

                    System.out.println(
                            "SPRING SECURITY AUTHENTICATION : FAILED"
                    );

                }

            } else {


                /*
                 * =================================================
                 * USER ALREADY AUTHENTICATED
                 * =================================================
                 */

                System.out.println(
                        "SECURITY CONTEXT ALREADY AUTHENTICATED"
                );

            }


        } catch (Exception exception) {


            /*
             * =================================================
             * JWT ERROR
             * =================================================
             */

            System.out.println(
                    "JWT AUTHENTICATION ERROR : " +
                    exception.getMessage()
            );


            /*
             * Clear invalid authentication.
             */

            SecurityContextHolder
                    .clearContext();
        }


        /*
         * =====================================================
         * CONTINUE REQUEST
         * ===================================================== */

        filterChain.doFilter(
                request,
                response
        );


        /*
         * =====================================================
         * RESPONSE STATUS
         * ===================================================== */

        System.out.println(
                "JWT FILTER RESPONSE STATUS : " +
                response.getStatus()
        );

        System.out.println(
                "================================================="
        );

        System.out.println();

    }
}