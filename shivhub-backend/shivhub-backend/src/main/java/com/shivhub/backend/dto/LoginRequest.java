package com.shivhub.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * LoginRequest
 *
 * This DTO receives login details from the frontend.
 *
 * User enters:
 * 1. Email
 * 2. Password
 *
 * These details are sent to the backend for authentication.
 */

public class LoginRequest {

    // User's email
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;

    // User's password
    @NotBlank(message = "Password is required")
    private String password;


    // Default constructor
    public LoginRequest() {
    }


    // Parameterized constructor
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }


    // Getter for email
    public String getEmail() {
        return email;
    }


    // Setter for email
    public void setEmail(String email) {
        this.email = email;
    }


    // Getter for password
    public String getPassword() {
        return password;
    }


    // Setter for password
    public void setPassword(String password) {
        this.password = password;
    }
}