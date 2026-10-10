package com.example.orders.order.dto;

import com.example.orders.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(Long id, Long customerId, String customerName, OrderStatus status,
                                   BigDecimal totalAmount, Instant createdAt, Instant updatedAt) {
}
