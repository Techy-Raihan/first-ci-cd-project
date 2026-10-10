package com.example.orders.customer;

import com.example.orders.customer.dto.CustomerRequest;
import com.example.orders.customer.dto.CustomerResponse;
import com.example.orders.shared.web.PageSupport;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private static final Set<String> SORTS = Set.of("id", "name", "email", "createdAt");
    private final CustomerService service;

    public CustomerController(CustomerService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create a customer", description = "Email addresses are unique ignoring case")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable long id) { return service.get(id); }

    @GetMapping
    public Page<CustomerResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(defaultValue = "createdAt") String sort,
                                       @RequestParam(defaultValue = "desc") String direction) {
        return service.list(PageSupport.create(page, size, sort, direction, SORTS));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }
}
