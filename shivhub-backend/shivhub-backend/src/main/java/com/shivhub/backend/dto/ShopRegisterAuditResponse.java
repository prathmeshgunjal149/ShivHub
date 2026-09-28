package com.shivhub.backend.dto;

import java.time.LocalDateTime;

public record ShopRegisterAuditResponse(Long id, Long entryId, String action, String actionByName,
                                        String previousSnapshot, String nextSnapshot, String reason,
                                        LocalDateTime createdAt) { }
