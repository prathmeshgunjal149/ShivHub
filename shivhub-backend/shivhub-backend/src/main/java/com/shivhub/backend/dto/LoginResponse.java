package com.shivhub.backend.dto;

import com.shivhub.backend.enums.Role;

/*
 * LoginResponse
 *
 * This DTO contains the information that the backend
 * sends back to the frontend after successful login.
 *
 * We will return:
 * 1. JWT token
 * 2. User ID
 * 3. User name
 * 4. User email
 * 5. User role
 */

public class LoginResponse {

    // JWT authentication token
    private String token;

    // Logged-in user's ID
    private Long userId;

    // Logged-in user's name
    private String name;

    // Logged-in user's email
    private String email;

    // Logged-in user's role
    private Role role;

    // Changed: signals the frontend to show the OTP form; token stays null until verification.
    private boolean otpRequired;


    // Default constructor
    public LoginResponse() {
    }


    // Parameterized constructor
    public LoginResponse(
            String token,
            Long userId,
            String name,
            String email,
            Role role) {

        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }


    // Getter for token
    public String getToken() {
        return token;
    }

    // Setter for token
    public void setToken(String token) {
        this.token = token;
    }


    // Getter for user ID
    public Long getUserId() {
        return userId;
    }

    // Setter for user ID
    public void setUserId(Long userId) {
        this.userId = userId;
    }


    // Getter for name
    public String getName() {
        return name;
    }

    // Setter for name
    public void setName(String name) {
        this.name = name;
    }


    // Getter for email
    public String getEmail() {
        return email;
    }

    // Setter for email
    public void setEmail(String email) {
        this.email = email;
    }


    // Getter for role
    public Role getRole() {
        return role;
    }

    // Setter for role
    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isOtpRequired() { return otpRequired; }
    public void setOtpRequired(boolean otpRequired) { this.otpRequired = otpRequired; }
}
