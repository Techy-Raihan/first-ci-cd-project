package com.example.orders.product;

import com.example.orders.product.dto.ProductResponse;

public final class ProductMapper {
    private ProductMapper() { }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStockQuantity(), product.isActive(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}
