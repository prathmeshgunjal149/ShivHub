package com.shivhub.backend.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shivhub.backend.entity.CustomerProfile;
import com.shivhub.backend.entity.User;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByOnlineUser(User user);
    Optional<CustomerProfile> findFirstByMobile(String mobile);
    Optional<CustomerProfile> findFirstByEmailIgnoreCase(String email);
    List<CustomerProfile> findByMobileIn(Collection<String> mobiles);

    @Query("""
            select profile from CustomerProfile profile
            join SellerCustomerMapping mapping on mapping.customerProfile = profile
            where mapping.seller = :seller and (
                 lower(profile.name) like lower(concat('%', :query, '%'))
              or lower(coalesce(profile.mobile, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(profile.email, '')) like lower(concat('%', :query, '%')))
            order by case when lower(profile.name) like lower(concat(:query, '%')) then 0 else 1 end,
                     profile.name asc
            """)
    List<CustomerProfile> searchSellerSuggestions(@Param("seller") User seller, @Param("query") String query, Pageable pageable);

    @Query("""
            select profile from CustomerProfile profile
            where lower(profile.name) like lower(concat('%', :query, '%'))
               or lower(coalesce(profile.mobile, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(profile.email, '')) like lower(concat('%', :query, '%'))
            order by case when lower(profile.name) like lower(concat(:query, '%')) then 0 else 1 end,
                     profile.name asc
            """)
    List<CustomerProfile> searchAdminSuggestions(@Param("query") String query, Pageable pageable);
}
