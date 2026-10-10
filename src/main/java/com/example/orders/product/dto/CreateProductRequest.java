package com.example.orders.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 64) @Schema(example = "LAPTOP-001") String sku,
        @NotBlank @Size(max = 160) @Schema(example = "Developer Laptop") String name,
        @Size(max = 1000) @Schema(example = "16 GB RAM laptop") String description,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
        @Schema(example = "79999.00") BigDecimal price,
        @NotNull @PositiveOrZero @Schema(example = "10") Integer stockQuantity) {
}
