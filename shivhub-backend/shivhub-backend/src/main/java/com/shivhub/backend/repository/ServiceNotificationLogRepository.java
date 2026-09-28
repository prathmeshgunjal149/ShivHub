package com.shivhub.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shivhub.backend.entity.ServiceNotificationLog;
public interface ServiceNotificationLogRepository extends JpaRepository<ServiceNotificationLog, Long> { }
