package com.example.orders.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productSku, String productName,
                                BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
}
