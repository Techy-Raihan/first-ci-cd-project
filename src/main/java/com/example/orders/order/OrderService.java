package com.example.orders.order;

import com.example.orders.customer.Customer;
import com.example.orders.customer.CustomerRepository;
import com.example.orders.order.dto.CreateOrderItemRequest;
import com.example.orders.order.dto.CreateOrderRequest;
import com.example.orders.order.dto.OrderResponse;
import com.example.orders.order.dto.OrderSummaryResponse;
import com.example.orders.product.Product;
import com.example.orders.product.ProductRepository;
import com.example.orders.shared.error.BadRequestException;
import com.example.orders.shared.error.ConflictException;
import com.example.orders.shared.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        rejectDuplicates(request.items());
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer " + request.customerId() + " was not found"));

        List<Long> ids = request.items().stream().map(CreateOrderItemRequest::productId).sorted().toList();
        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(ids);
        if (lockedProducts.size() != ids.size()) {
            Set<Long> found = lockedProducts.stream().map(Product::getId).collect(Collectors.toSet());
            Long missing = ids.stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new NotFoundException("Product " + missing + " was not found");
        }
        Map<Long, Product> products = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        PurchaseOrder order = new PurchaseOrder(customer);
        for (CreateOrderItemRequest requested : request.items()) {
            Product product = products.get(requested.productId());
            if (!product.isActive()) {
                throw new ConflictException("INACTIVE_PRODUCT", "Product " + product.getId() + " is inactive");
            }
            if (product.getStockQuantity() < requested.quantity()) {
                throw new ConflictException("INSUFFICIENT_STOCK",
                        "Product " + product.getId() + " has only " + product.getStockQuantity() + " units available");
            }
            product.deductStock(requested.quantity());
            order.addItem(new OrderItem(product, requested.quantity()));
        }
        PurchaseOrder saved = orderRepository.save(order);
        log.info("Created order id={} customerId={} itemCount={} total={}", saved.getId(), customer.getId(),
                saved.getItems().size(), saved.getTotalAmount());
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long id) { return OrderMapper.toResponse(requireDetailed(id)); }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> list(Long customerId, OrderStatus status, Pageable pageable) {
        if (customerId != null && !customerRepository.existsById(customerId)) {
            throw new NotFoundException("Customer " + customerId + " was not found");
        }
        return orderRepository.search(customerId, status, pageable).map(OrderMapper::toSummary);
    }

    @Transactional
    public OrderResponse confirm(long id) { return transition(id, OrderStatus.CREATED, OrderStatus.CONFIRMED); }

    @Transactional
    public OrderResponse ship(long id) { return transition(id, OrderStatus.CONFIRMED, OrderStatus.SHIPPED); }

    @Transactional
    public OrderResponse deliver(long id) { return transition(id, OrderStatus.SHIPPED, OrderStatus.DELIVERED); }

    @Transactional
    public OrderResponse cancel(long id) {
        PurchaseOrder order = requireForUpdate(id);
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw invalidTransition(order.getStatus(), OrderStatus.CANCELLED);
        }
        List<Long> productIds = order.getItems().stream().map(i -> i.getProduct().getId()).sorted().toList();
        Map<Long, Product> locked = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            locked.get(item.getProduct().getId()).restoreStock(item.getQuantity());
        }
        order.changeStatus(OrderStatus.CANCELLED);
        log.info("Cancelled order id={} and restored stock for {} items", id, order.getItems().size());
        return OrderMapper.toResponse(order);
    }

    private OrderResponse transition(long id, OrderStatus expected, OrderStatus target) {
        PurchaseOrder order = requireForUpdate(id);
        if (order.getStatus() != expected) throw invalidTransition(order.getStatus(), target);
        order.changeStatus(target);
        log.info("Changed order id={} status from {} to {}", id, expected, target);
        return OrderMapper.toResponse(order);
    }

    private PurchaseOrder requireDetailed(long id) {
        return orderRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " was not found"));
    }
    private PurchaseOrder requireForUpdate(long id) {
        return orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " was not found"));
    }
    private static void rejectDuplicates(List<CreateOrderItemRequest> items) {
        Set<Long> seen = new HashSet<>();
        for (CreateOrderItemRequest item : items) {
            if (!seen.add(item.productId())) {
                throw new BadRequestException("DUPLICATE_ORDER_ITEM",
                        "Product " + item.productId() + " appears more than once; combine it into one item");
            }
        }
    }
    private static ConflictException invalidTransition(OrderStatus from, OrderStatus to) {
        return new ConflictException("INVALID_ORDER_TRANSITION", "Order cannot move from " + from + " to " + to);
    }
}
