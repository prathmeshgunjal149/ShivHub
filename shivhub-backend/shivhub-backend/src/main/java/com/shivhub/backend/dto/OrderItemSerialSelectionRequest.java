package com.shivhub.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSerialSelectionRequest {

    @NotNull
    private Long orderItemId;

    @NotNull
    private List<Long> serialIds;
}
