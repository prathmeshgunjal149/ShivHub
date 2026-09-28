package com.shivhub.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shivhub.backend.entity.SiteFaq;

public interface SiteFaqRepository extends JpaRepository<SiteFaq, Long> {
    List<SiteFaq> findByActiveTrueOrderByDisplayOrderAscIdAsc();
    List<SiteFaq> findAllByOrderByDisplayOrderAscIdAsc();
}
