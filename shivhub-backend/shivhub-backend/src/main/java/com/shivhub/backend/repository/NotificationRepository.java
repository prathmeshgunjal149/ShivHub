package com.shivhub.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> { }
