package com.example.orders.order;

import com.example.orders.order.dto.OrderItemResponse;
import com.example.orders.order.dto.OrderResponse;
import com.example.orders.order.dto.OrderSummaryResponse;

import java.util.List;

public final class OrderMapper {
    private OrderMapper() { }

    public static OrderResponse toResponse(PurchaseOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProductSku(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getCustomer().getId(), order.getCustomer().getName(),
                order.getStatus(), order.getTotalAmount(), items, order.getCreatedAt(), order.getUpdatedAt());
    }

    public static OrderSummaryResponse toSummary(PurchaseOrder order) {
        return new OrderSummaryResponse(order.getId(), order.getCustomer().getId(), order.getCustomer().getName(),
                order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
