package com.shivhub.backend.dto;

import java.time.LocalDate;
import lombok.Data;

/** Revised customer promise date after a partial collection. */
@Data
public class CustomerDueDateRequest {
    private LocalDate dueDate;
    private String notes;
}
