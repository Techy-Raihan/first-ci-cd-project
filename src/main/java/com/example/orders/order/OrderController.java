package com.example.orders.order;

import com.example.orders.order.dto.CreateOrderRequest;
import com.example.orders.order.dto.OrderResponse;
import com.example.orders.order.dto.OrderSummaryResponse;
import com.example.orders.shared.web.PageSupport;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private static final Set<String> SORTS = Set.of("id", "status", "totalAmount", "createdAt", "updatedAt");
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create an order", description = "Locks products, checks stock, calculates totals, and deducts stock atomically. Duplicate product IDs are rejected.")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable long id) { return service.get(id); }

    @GetMapping
    public Page<OrderSummaryResponse> list(@RequestParam(required = false) Long customerId,
                                           @RequestParam(required = false) OrderStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(defaultValue = "createdAt") String sort,
                                           @RequestParam(defaultValue = "desc") String direction) {
        return service.list(customerId, status, PageSupport.create(page, size, sort, direction, SORTS));
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable long id) { return service.confirm(id); }

    @PostMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable long id) { return service.ship(id); }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable long id) { return service.deliver(id); }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable long id) { return service.cancel(id); }
}
