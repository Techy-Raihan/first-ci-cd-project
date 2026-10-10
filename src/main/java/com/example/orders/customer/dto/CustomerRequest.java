package com.example.orders.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 160) @Schema(example = "Ravi Kumar") String name,
        @NotBlank @Email @Size(max = 254) @Schema(example = "ravi@example.com") String email,
        @NotBlank @Size(max = 32) @Schema(example = "+91 98765 43210") String phone,
        @NotBlank @Size(max = 500) @Schema(example = "12 Park Road, Pune, Maharashtra 411001") String deliveryAddress) {
}
