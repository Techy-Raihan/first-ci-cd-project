package com.example.orders.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(Long id, String sku, String name, String description, BigDecimal price,
                              int stockQuantity, boolean active, Instant createdAt, Instant updatedAt) {
}
