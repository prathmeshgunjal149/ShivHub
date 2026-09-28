package com.shivhub.backend.dto;

import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;


/*
 * =========================================================
 * UserResponse
 * =========================================================
 *
 * This DTO is used to safely send user information
 * from backend to frontend.
 *
 * IMPORTANT:
 *
 * Password is intentionally NOT included.
 *
 * Even the BCrypt password hash should NEVER be
 * returned through an API response.
 *
 * =========================================================
 */

public class UserResponse {


    /*
     * User's unique ID.
     */

    private Long id;


    /*
     * User's full name.
     */

    private String name;


    /*
     * User's email.
     */

    private String email;


    /*
     * User's mobile number.
     */

    private String mobile;


    /*
     * User's role.
     *
     * CUSTOMER
     * SELLER
     * ADMIN
     */

    private Role role;


    /*
     * User's account status.
     *
     * PENDING
     * APPROVED
     * REJECTED
     */

    private UserStatus status;

    /** Whether the account can currently sign in and use ShivHub. */
    private boolean enabled;

    private String rejectionReason;


    /*
     * =========================================================
     * DEFAULT CONSTRUCTOR
     * =========================================================
     */

    public UserResponse() {
    }


    /*
     * =========================================================
     * PARAMETERIZED CONSTRUCTOR
     * =========================================================
     *
     * Used by AuthService and AdminService.
     *
     * Notice:
     *
     * Password is NOT present.
     *
     * =========================================================
     */

    public UserResponse(
            Long id,
            String name,
            String email,
            String mobile,
            Role role,
            UserStatus status) {

        this.id = id;

        this.name = name;

        this.email = email;

        this.mobile = mobile;

        this.role = role;

        this.status = status;
    }


    /*
     * =========================================================
     * GETTER / SETTER - ID
     * =========================================================
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    /*
     * =========================================================
     * GETTER / SETTER - NAME
     * =========================================================
     */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    /*
     * =========================================================
     * GETTER / SETTER - EMAIL
     * =========================================================
     */

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    /*
     * =========================================================
     * GETTER / SETTER - MOBILE
     * =========================================================
     */

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }


    /*
     * =========================================================
     * GETTER / SETTER - ROLE
     * =========================================================
     */

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }


    /*
     * =========================================================
     * GETTER / SETTER - STATUS
     * =========================================================
     */

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRejectionReason() { return rejectionReason; }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
