package com.example.orders.order;

import com.example.orders.customer.Customer;
import com.example.orders.shared.entity.AuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
public class PurchaseOrder extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private final List<OrderItem> items = new ArrayList<>();

    protected PurchaseOrder() { }

    public PurchaseOrder(Customer customer) { this.customer = customer; }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        items.add(item);
        totalAmount = totalAmount.add(item.getLineTotal());
    }

    public void changeStatus(OrderStatus status) { this.status = status; }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public long getVersion() { return version; }
}
