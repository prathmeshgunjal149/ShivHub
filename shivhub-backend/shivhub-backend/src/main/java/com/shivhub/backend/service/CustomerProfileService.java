package com.shivhub.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

import com.shivhub.backend.dto.CustomerProfileResponse;
import com.shivhub.backend.dto.UpdateCustomerProfileRequest;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.repository.UserRepository;
import com.shivhub.backend.repository.CustomerProfileRepository;
import com.shivhub.backend.entity.CustomerProfile;

@Service
public class CustomerProfileService {


    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CustomerProfileRepository customerProfiles;


    public CustomerProfileService(
            UserRepository userRepository,
            FileStorageService fileStorageService,
            CustomerProfileRepository customerProfiles
    ) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.customerProfiles = customerProfiles;
    }


    /*
     * =========================================================
     * GET CUSTOMER PROFILE
     * =========================================================
     *
     * JWT authentication मधून email येतो.
     *
     * email
     *   ↓
     * UserRepository
     *   ↓
     * Customer
     *
     * =========================================================
     */

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(
            String email
    ) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );


        /*
         * Only CUSTOMER can access this profile.
         */

        if (user.getRole() != Role.CUSTOMER) {

            throw new RuntimeException(
                    "Only customers can access this profile"
            );
        }


        return mapToResponse(user);
    }



    /*
     * =========================================================
     * UPDATE CUSTOMER PROFILE
     * =========================================================
     */

    @Transactional
    public CustomerProfileResponse updateProfile(
            String email,
            UpdateCustomerProfileRequest request
    ) {


        /*
         * Find logged-in customer
         */

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found"
                                )
                        );


        /*
         * Make sure user is CUSTOMER.
         */

        if (user.getRole() != Role.CUSTOMER) {

            throw new RuntimeException(
                    "Only customers can update this profile"
            );
        }


        /*
         * =====================================================
         * NULL REQUEST CHECK
         * =====================================================
         */

        if (request == null) {

            throw new RuntimeException(
                    "Profile update data is required"
            );
        }



        /*
         * =====================================================
         * UPDATE NAME
         * =====================================================
         */

        if (request.getName() != null) {

            String name =
                    request.getName().trim();


            if (name.isEmpty()) {

                throw new RuntimeException(
                        "Name cannot be empty"
                );
            }


            if (name.length() < 2) {

                throw new RuntimeException(
                        "Name must contain at least 2 characters"
                );
            }


            user.setName(name);
        }



        /*
         * =====================================================
         * UPDATE MOBILE
         * =====================================================
         */

        if (request.getMobile() != null) {

            String mobile =
                    request.getMobile().trim();


            /*
             * Empty mobile means remove mobile number.
             */

            if (mobile.isEmpty()) {

                user.setMobile(null);

            } else {


                /*
                 * Indian 10 digit mobile validation.
                 *
                 * Starts with 6, 7, 8 or 9.
                 */

                if (!mobile.matches(
                        "^[6-9][0-9]{9}$"
                )) {

                    throw new RuntimeException(
                            "Enter a valid 10 digit mobile number"
                    );
                }


                /*
                 * =================================================
                 * DUPLICATE MOBILE CHECK
                 * =================================================
                 *
                 * We already have existsByMobile()
                 * in UserRepository.
                 *
                 * If the customer is keeping the same
                 * mobile number, don't reject it.
                 *
                 * =================================================
                 */

                String currentMobile =
                        user.getMobile();


                boolean mobileChanged =
                        currentMobile == null ||
                        !currentMobile.equals(mobile);


                if (mobileChanged &&
                        userRepository
                                .existsByMobile(mobile)) {

                    throw new RuntimeException(
                            "Mobile number is already registered"
                    );
                }


                user.setMobile(mobile);
            }
        }

        if (request.getProfilePhotoUrl() != null) {
            String photoUrl = request.getProfilePhotoUrl().trim();
            if (photoUrl.isEmpty()) {
                user.setProfilePhotoUrl(null);
            } else {
                if (photoUrl.length() > 2000 ||
                        !(photoUrl.startsWith("http://")
                                || photoUrl.startsWith("https://")
                                || photoUrl.startsWith("/uploads/"))) {
                    throw new RuntimeException("Profile photo URL must be a valid HTTP(S) or uploaded image URL");
                }
                user.setProfilePhotoUrl(photoUrl);
            }
        }

        if (request.getDateOfBirth() != null) {
            validateDateOfBirth(request.getDateOfBirth());
            user.setDateOfBirth(request.getDateOfBirth());
            customerProfiles.findByOnlineUser(user).ifPresent(profile -> { profile.setDateOfBirth(request.getDateOfBirth()); customerProfiles.save(profile); });
        }



        /*
         * =====================================================
         * SAVE USER
         * =====================================================
         */

        User savedUser =
                userRepository.save(user);



        /*
         * =====================================================
         * RETURN UPDATED PROFILE
         * =====================================================
         */

        return mapToResponse(savedUser);
    }



    /*
     * =========================================================
     * MAP USER ENTITY → PROFILE RESPONSE
     * =========================================================
     *
     * Password is intentionally NOT returned.
     *
     * =========================================================
     */

    private CustomerProfileResponse mapToResponse(
            User user
    ) {

        return new CustomerProfileResponse(

                user.getId(),

                user.getName(),

                user.getEmail(),

                user.getMobile(),

                user.getProfilePhotoUrl(),

                user.isMarketingOptOut(),

                user.getDateOfBirth(),

                user.getRole(),

                user.getStatus(),

                user.isEnabled(),

                user.getCreatedAt(),

                user.getUpdatedAt()
        );
    }

    @Transactional
    public CustomerProfileResponse uploadProfilePhoto(String email, org.springframework.web.multipart.MultipartFile file) {
        User user = customer(email);
        String url = fileStorageService.storeCustomerProfilePhoto(user.getId(), file);
        user.setProfilePhotoUrl(url);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public CustomerProfileResponse removeProfilePhoto(String email) {
        User user = customer(email);
        user.setProfilePhotoUrl(null);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public CustomerProfileResponse updateMarketingPreference(String email, boolean optOut) {
        User user = customer(email);
        user.setMarketingOptOut(optOut);
        return mapToResponse(userRepository.save(user));
    }

    private void validateDateOfBirth(LocalDate value) {
        if (value.isAfter(LocalDate.now())) throw new IllegalArgumentException("Date of birth cannot be in the future");
        if (value.isBefore(LocalDate.now().minusYears(120))) throw new IllegalArgumentException("Enter a reasonable date of birth");
    }

    private User customer(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new RuntimeException("Only customers can update this profile");
        }
        return user;
    }

}
