package com.example.orders.order;

import com.example.orders.customer.Customer;
import com.example.orders.customer.CustomerRepository;
import com.example.orders.order.dto.CreateOrderItemRequest;
import com.example.orders.order.dto.CreateOrderRequest;
import com.example.orders.order.dto.OrderResponse;
import com.example.orders.product.Product;
import com.example.orders.product.ProductRepository;
import com.example.orders.shared.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private OrderRepository orders;
    private ProductRepository products;
    private CustomerRepository customers;
    private OrderService service;
    private Customer customer;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        products = mock(ProductRepository.class);
        customers = mock(CustomerRepository.class);
        service = new OrderService(orders, products, customers);
        customer = new Customer("Ravi", "ravi@example.com", "9999999999", "Pune");
        ReflectionTestUtils.setField(customer, "id", 10L);
        when(customers.findById(10L)).thenReturn(Optional.of(customer));
        when(orders.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void calculatesTotalsOnServerAndPreservesPriceSnapshot() {
        Product product = product(1L, "SKU-1", "Keyboard", "125.50", 10, true);
        when(products.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(product));

        OrderResponse response = service.create(request(item(1L, 3)));
        product.update("Renamed", null, new BigDecimal("999.00"), product.getStockQuantity());

        assertThat(response.totalAmount()).isEqualByComparingTo("376.50");
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("125.50");
        assertThat(response.items().getFirst().productName()).isEqualTo("Keyboard");
        PurchaseOrder savedOrder = (PurchaseOrder) mockingDetails(orders).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save"))
                .findFirst().orElseThrow().getArgument(0);
        assertThat(savedOrder.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("125.50");
        assertThat(savedOrder.getItems().getFirst().getProductName()).isEqualTo("Keyboard");
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void rejectsInsufficientStockAndDoesNotSaveOrder() {
        when(products.findAllByIdForUpdate(List.of(1L)))
                .thenReturn(List.of(product(1L, "SKU-1", "Keyboard", "100.00", 1, true)));

        assertThatThrownBy(() -> service.create(request(item(1L, 2))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("INSUFFICIENT_STOCK");
        verify(orders, never()).save(any());
    }

    @Test
    void rejectsInactiveProduct() {
        when(products.findAllByIdForUpdate(List.of(1L)))
                .thenReturn(List.of(product(1L, "SKU-1", "Keyboard", "100.00", 5, false)));

        assertThatThrownBy(() -> service.create(request(item(1L, 1))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("INACTIVE_PRODUCT");
        verify(orders, never()).save(any());
    }

    @Test
    void rejectsDuplicateProductEntries() {
        assertThatThrownBy(() -> service.create(request(item(1L, 1), item(1L, 2))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("DUPLICATE_ORDER_ITEM");
        verifyNoInteractions(products);
    }

    @Test
    void partialFailureDoesNotSaveOrderSoTransactionCanRollBack() {
        Product first = product(1L, "SKU-1", "First", "10.00", 5, true);
        Product second = product(2L, "SKU-2", "Second", "20.00", 0, true);
        when(products.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.create(request(item(1L, 1), item(2L, 1))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("INSUFFICIENT_STOCK");
        verify(orders, never()).save(any());
    }

    @Test
    void supportsValidTransitionsAndRejectsInvalidOne() {
        PurchaseOrder order = new PurchaseOrder(customer);
        ReflectionTestUtils.setField(order, "id", 20L);
        when(orders.findByIdForUpdate(20L)).thenReturn(Optional.of(order));

        assertThat(service.confirm(20L).status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(service.ship(20L).status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(service.deliver(20L).status()).isEqualTo(OrderStatus.DELIVERED);
        assertThatThrownBy(() -> service.cancel(20L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("INVALID_ORDER_TRANSITION");
    }

    @Test
    void cancellationRestoresStockExactlyOnce() {
        Product product = product(1L, "SKU-1", "Keyboard", "100.00", 3, true);
        PurchaseOrder order = new PurchaseOrder(customer);
        order.addItem(new OrderItem(product, 2));
        ReflectionTestUtils.setField(order, "id", 20L);
        when(orders.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(products.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(product));

        assertThat(service.cancel(20L).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThatThrownBy(() -> service.cancel(20L)).isInstanceOf(ApiException.class);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        verify(products, times(1)).findAllByIdForUpdate(List.of(1L));
    }

    private CreateOrderRequest request(CreateOrderItemRequest... items) {
        return new CreateOrderRequest(10L, List.of(items));
    }
    private CreateOrderItemRequest item(long id, int quantity) { return new CreateOrderItemRequest(id, quantity); }
    private Product product(long id, String sku, String name, String price, int stock, boolean active) {
        Product product = new Product(sku, name, null, new BigDecimal(price), stock);
        ReflectionTestUtils.setField(product, "id", id);
        if (!active) product.deactivate();
        return product;
    }
}
