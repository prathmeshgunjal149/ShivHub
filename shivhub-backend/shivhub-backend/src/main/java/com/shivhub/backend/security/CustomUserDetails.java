package com.shivhub.backend.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.shivhub.backend.entity.User;

/*
 * CustomUserDetails
 *
 * Spring Security ला आपल्या ShivHub User entity बद्दल
 * information समजण्यासाठी ही class वापरतो.
 *
 * Spring Security ला UserDetails object पाहिजे असतो.
 * त्यामुळे आपला User object UserDetails मध्ये convert करतो.
 */

public class CustomUserDetails implements UserDetails {

    private final User user;

    /*
     * Constructor
     *
     * Existing ShivHub User object घेतो.
     */
    public CustomUserDetails(User user) {
        this.user = user;
    }

    /*
     * User's authorities / role
     *
     * Example:
     *
     * CUSTOMER → ROLE_CUSTOMER
     * SELLER   → ROLE_SELLER
     * ADMIN    → ROLE_ADMIN
     *
     * Spring Security role-based authorization साठी
     * हे वापरेल.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return Collections.singletonList(
                new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name()
                )
        );
    }

    /*
     * Password
     *
     * Database मध्ये stored BCrypt password return करतो.
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /*
     * Username
     *
     * आपल्या application मध्ये login email ने होणार आहे.
     *
     * म्हणून email ला username म्हणून return करतो.
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /*
     * Account expiration
     *
     * सध्या account expiration feature नाही.
     * त्यामुळे true.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /*
     * Account locked आहे का?
     *
     * सध्या account locking feature नाही.
     * त्यामुळे true.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /*
     * Credentials expired आहेत का?
     *
     * सध्या password expiration feature नाही.
     * त्यामुळे true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /*
     * Account enabled आहे का?
     *
     * User entity मधील enabled field वापरतो.
     *
     * Admin एखादा account disable करू शकतो.
     */
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    /*
     * Original User object मिळवण्यासाठी helper method.
     *
     * भविष्यात user ID / role / name इत्यादी
     * information access करण्यासाठी उपयोगी.
     */
    public User getUser() {
        return user;
    }
}