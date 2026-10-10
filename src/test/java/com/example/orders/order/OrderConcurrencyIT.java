package com.example.orders.order;

import com.example.orders.customer.Customer;
import com.example.orders.customer.CustomerRepository;
import com.example.orders.order.dto.CreateOrderItemRequest;
import com.example.orders.order.dto.CreateOrderRequest;
import com.example.orders.product.Product;
import com.example.orders.product.ProductRepository;
import com.example.orders.shared.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("postgres-it")
class OrderConcurrencyIT {
    @Autowired OrderService orderService;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetDedicatedTestDatabase() {
        String database = jdbc.queryForObject("select current_database()", String.class);
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalStateException("Refusing integration-test cleanup: database name must end with _test");
        }
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
        jdbc.update("delete from products");
        jdbc.update("delete from customers");
    }

    @Test
    void failedMultiItemOrderRollsBackEarlierStockDeduction() {
        Customer customer = customers.save(new Customer("Test", "rollback@example.com", "999", "Test address"));
        Product first = products.save(new Product("ROLL-1", "First", null, new BigDecimal("10.00"), 5));
        Product empty = products.save(new Product("ROLL-2", "Empty", null, new BigDecimal("20.00"), 0));

        CreateOrderRequest request = new CreateOrderRequest(customer.getId(), List.of(
                new CreateOrderItemRequest(first.getId(), 1), new CreateOrderItemRequest(empty.getId(), 1)));
        assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(ApiException.class);

        assertThat(products.findById(first.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(orders.count()).isZero();
    }

    @Test
    void concurrentOrdersCannotOversellStock() throws Exception {
        Customer customer = customers.save(new Customer("Test", "concurrent@example.com", "999", "Test address"));
        Product product = products.save(new Product("LOCK-1", "Locked", null, new BigDecimal("10.00"), 5));
        CreateOrderRequest request = new CreateOrderRequest(customer.getId(),
                List.of(new CreateOrderItemRequest(product.getId(), 4)));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> task = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                orderService.create(request);
                return true;
            } catch (ApiException ex) {
                return false;
            }
        };
        try {
            Future<Boolean> first = executor.submit(task);
            Future<Boolean> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void concurrentCancellationRestoresStockOnlyOnce() throws Exception {
        Customer customer = customers.save(new Customer("Test", "cancel@example.com", "999", "Test address"));
        Product product = products.save(new Product("CANCEL-1", "Cancel", null, new BigDecimal("10.00"), 5));
        Long orderId = orderService.create(new CreateOrderRequest(customer.getId(),
                List.of(new CreateOrderItemRequest(product.getId(), 3)))).id();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> task = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                orderService.cancel(orderId);
                return true;
            } catch (ApiException ex) {
                return false;
            }
        };
        try {
            Future<Boolean> first = executor.submit(task);
            Future<Boolean> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
