package com.shivhub.backend.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;


/*
 * =========================================================
 * UserRepository
 * =========================================================
 *
 * Responsible for communicating with the "users" table.
 *
 * =========================================================
 */

public interface UserRepository
        extends JpaRepository<User, Long> {


    /*
     * =========================================================
     * FIND USER BY EMAIL
     * =========================================================
     *
     * Used by:
     *
     * - Login
     * - JWT authentication
     * - Customer profile
     *
     * =========================================================
     */

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);



    /*
     * =========================================================
     * CHECK DUPLICATE EMAIL
     * =========================================================
     */

    boolean existsByEmail(String email);



    /*
     * =========================================================
     * CHECK DUPLICATE MOBILE
     * =========================================================
     *
     * Used while updating customer profile.
     *
     * =========================================================
     */

    boolean existsByMobile(String mobile);

    Optional<User> findByMobile(String mobile);

    List<User> findByMobileIn(Collection<String> mobiles);

    Optional<User> findByReferralCodeIgnoreCase(String referralCode);

    boolean existsByReferralCodeIgnoreCase(String referralCode);



    /*
     * =========================================================
     * FIND USERS BY ROLE AND STATUS
     * =========================================================
     *
     * Existing seller approval functionality.
     *
     * Example:
     *
     * SELLER + PENDING
     *
     * =========================================================
     */

    List<User> findByRoleAndStatus(
            Role role,
            UserStatus status
    );



    /*
     * =========================================================
     * FIND USERS BY ROLE
     * =========================================================
     *
     * Used by Admin Customer Management.
     *
     * Example:
     *
     * Role.CUSTOMER
     *
     * Returns all customers.
     *
     * =========================================================
     */

    List<User> findByRole(
            Role role
    );



    /*
     * =========================================================
     * FIND USERS BY ROLE AND ENABLED
     * =========================================================
     *
     * Used for:
     *
     * - Active customers
     * - Blocked customers
     *
     * =========================================================
     */

    List<User> findByRoleAndEnabled(
            Role role,
            boolean enabled
    );

    long countByRole(Role role);

    @Query("""
            select user from User user
            where user.role = :role and (
                 lower(user.name) like lower(concat('%', :query, '%'))
              or lower(coalesce(user.businessName, '')) like lower(concat('%', :query, '%'))
              or lower(user.email) like lower(concat('%', :query, '%'))
              or lower(coalesce(user.mobile, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(user.gstin, '')) like lower(concat('%', :query, '%')))
            order by case when lower(user.name) like lower(concat(:query, '%')) then 0 else 1 end,
                     user.name asc
            """)
    List<User> searchByRoleForSuggestions(@Param("role") Role role, @Param("query") String query, Pageable pageable);

}
