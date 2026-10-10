package com.example.orders.customer;

import com.example.orders.customer.dto.CustomerResponse;

public final class CustomerMapper {
    private CustomerMapper() { }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone(),
                customer.getDeliveryAddress(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
