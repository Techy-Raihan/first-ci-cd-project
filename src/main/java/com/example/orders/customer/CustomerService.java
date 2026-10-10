package com.example.orders.customer;

import com.example.orders.customer.dto.CustomerRequest;
import com.example.orders.customer.dto.CustomerResponse;
import com.example.orders.shared.error.ConflictException;
import com.example.orders.shared.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) { this.repository = repository; }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) throw duplicateEmail(email);
        try {
            Customer saved = repository.save(new Customer(request.name().trim(), email,
                    request.phone().trim(), request.deliveryAddress().trim()));
            log.info("Created customer id={}", saved.getId());
            return CustomerMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateEmail(email);
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(long id) { return CustomerMapper.toResponse(require(id)); }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(Pageable pageable) { return repository.findAll(pageable).map(CustomerMapper::toResponse); }

    @Transactional
    public CustomerResponse update(long id, CustomerRequest request) {
        Customer customer = require(id);
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) throw duplicateEmail(email);
        customer.update(request.name().trim(), email, request.phone().trim(), request.deliveryAddress().trim());
        try {
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw duplicateEmail(email);
        }
        log.info("Updated customer id={}", id);
        return CustomerMapper.toResponse(customer);
    }

    Customer require(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " was not found"));
    }

    private static String normalizeEmail(String value) { return value.trim().toLowerCase(); }
    private static ConflictException duplicateEmail(String email) {
        return new ConflictException("DUPLICATE_EMAIL", "A customer with email " + email + " already exists");
    }
}
