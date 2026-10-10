package com.example.orders.customer.dto;

import java.time.Instant;

public record CustomerResponse(Long id, String name, String email, String phone, String deliveryAddress,
                               Instant createdAt, Instant updatedAt) {
}
