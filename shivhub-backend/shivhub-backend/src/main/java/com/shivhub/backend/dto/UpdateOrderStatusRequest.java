package com.shivhub.backend.dto;

import java.util.List;

import com.shivhub.backend.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/*
 * =========================================================
 * UpdateOrderStatusRequest
 * =========================================================
 *
 * Used by Admin to update order status.
 *
 * =========================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull
    private OrderStatus orderStatus;

    private String deliveryPersonName;

    private String deliveryPersonMobile;

    private List<OrderItemSerialSelectionRequest> serialSelections;
}
