package com.example.orders.order.dto;

import com.example.orders.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Long customerId, String customerName, OrderStatus status,
                            BigDecimal totalAmount, List<OrderItemResponse> items,
                            Instant createdAt, Instant updatedAt) {
}
