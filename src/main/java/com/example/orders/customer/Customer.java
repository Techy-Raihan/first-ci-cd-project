package com.example.orders.customer;

import com.example.orders.shared.entity.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    protected Customer() { }

    public Customer(String name, String email, String phone, String deliveryAddress) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
    }

    public void update(String name, String email, String phone, String deliveryAddress) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.deliveryAddress = deliveryAddress;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDeliveryAddress() { return deliveryAddress; }
}
