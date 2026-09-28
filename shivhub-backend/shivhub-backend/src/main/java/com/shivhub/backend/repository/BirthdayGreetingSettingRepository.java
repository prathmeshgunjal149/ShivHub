package com.shivhub.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.BirthdayGreetingSetting;

public interface BirthdayGreetingSettingRepository extends JpaRepository<BirthdayGreetingSetting, Long> {
    Optional<BirthdayGreetingSetting> findFirstByActiveTrueOrderByIdDesc();
    /** Returns the setting the administrator last saved, including a deliberately disabled one. */
    Optional<BirthdayGreetingSetting> findFirstByOrderByIdDesc();
}
