package com.example.orders.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull @Positive @Schema(example = "1") Long productId,
        @NotNull @Positive @Schema(example = "2") Integer quantity) {
}
