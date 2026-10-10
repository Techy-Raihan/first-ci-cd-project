package com.example.orders.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Size(max = 160) @Schema(example = "Developer Laptop Pro") String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
        @Schema(example = "84999.00") BigDecimal price,
        @NotNull @PositiveOrZero @Schema(example = "8") Integer stockQuantity) {
}
