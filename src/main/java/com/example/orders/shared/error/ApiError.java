package com.example.orders.shared.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Consistent API error response")
public record ApiError(
        @Schema(example = "2026-10-10T10:15:30Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "VALIDATION_FAILED") String errorCode,
        @Schema(example = "Request validation failed") String message,
        @Schema(example = "/api/v1/products") String path,
        Map<String, String> fieldErrors) {
}
