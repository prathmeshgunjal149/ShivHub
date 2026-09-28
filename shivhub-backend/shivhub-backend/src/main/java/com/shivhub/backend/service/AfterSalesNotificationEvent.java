package com.shivhub.backend.service;

import com.shivhub.backend.enums.AfterSalesNotificationType;

public record AfterSalesNotificationEvent(Long requestId, AfterSalesNotificationType type, String remarks) { }
