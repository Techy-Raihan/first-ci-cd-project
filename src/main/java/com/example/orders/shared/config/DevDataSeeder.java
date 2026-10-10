package com.example.orders.shared.config;

import com.example.orders.customer.Customer;
import com.example.orders.customer.CustomerRepository;
import com.example.orders.product.Product;
import com.example.orders.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private final ProductRepository products;
    private final CustomerRepository customers;

    public DevDataSeeder(ProductRepository products, CustomerRepository customers) {
        this.products = products;
        this.customers = customers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedProduct("KEYBOARD-001", "Mechanical Keyboard", "Developer keyboard", "4999.00", 25);
        seedProduct("MOUSE-001", "Wireless Mouse", "Ergonomic mouse", "1999.00", 40);
        seedProduct("MONITOR-001", "27-inch Monitor", "QHD display", "24999.00", 12);
        seedCustomer("Demo Customer", "demo.customer@example.com", "+91 90000 00000",
                "Demo address, Pune, Maharashtra 411001");
        log.info("Development sample data check completed");
    }

    private void seedProduct(String sku, String name, String description, String price, int stock) {
        if (!products.existsBySkuIgnoreCase(sku)) {
            products.save(new Product(sku, name, description, new BigDecimal(price), stock));
        }
    }

    private void seedCustomer(String name, String email, String phone, String address) {
        if (!customers.existsByEmailIgnoreCase(email)) {
            customers.save(new Customer(name, email, phone, address));
        }
    }
}
