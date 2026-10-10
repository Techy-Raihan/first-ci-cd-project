package com.example.orders.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull @Positive @Schema(example = "1") Long customerId,
        @NotEmpty @Size(max = 50) List<@Valid CreateOrderItemRequest> items) {
}
