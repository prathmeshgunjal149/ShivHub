package com.shivhub.backend.dto;

import java.util.List;

/** Stable pagination envelope shared by the admin management screens. */
public record AdminPageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) { }
