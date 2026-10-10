package com.example.orders.product;

import com.example.orders.product.dto.CreateProductRequest;
import com.example.orders.product.dto.ProductResponse;
import com.example.orders.product.dto.UpdateProductRequest;
import com.example.orders.shared.error.ApiError;
import com.example.orders.shared.web.PageSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private static final Set<String> SORTS = Set.of("id", "sku", "name", "price", "stockQuantity", "createdAt");
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create a product")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "409", description = "SKU already exists",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable long id) { return service.get(id); }

    @GetMapping
    public Page<ProductResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(defaultValue = "createdAt") String sort,
                                      @RequestParam(defaultValue = "desc") String direction) {
        return service.list(search, PageSupport.create(page, size, sort, direction, SORTS));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable long id, @Valid @RequestBody UpdateProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) { service.deactivate(id); }
}
