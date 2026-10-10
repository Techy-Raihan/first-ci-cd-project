package com.example.orders.product;

import com.example.orders.product.dto.CreateProductRequest;
import com.example.orders.product.dto.ProductResponse;
import com.example.orders.product.dto.UpdateProductRequest;
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
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) { this.repository = repository; }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (repository.existsBySkuIgnoreCase(sku)) throw duplicateSku(sku);
        try {
            Product saved = repository.save(new Product(sku, request.name().trim(), trimToNull(request.description()),
                    request.price(), request.stockQuantity()));
            log.info("Created product id={} sku={}", saved.getId(), saved.getSku());
            return ProductMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateSku(sku);
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse get(long id) { return ProductMapper.toResponse(require(id)); }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String search, Pageable pageable) {
        String normalized = search == null || search.isBlank() ? null : search.trim();
        return repository.search(normalized, pageable).map(ProductMapper::toResponse);
    }

    @Transactional
    public ProductResponse update(long id, UpdateProductRequest request) {
        Product product = require(id);
        product.update(request.name().trim(), trimToNull(request.description()), request.price(), request.stockQuantity());
        log.info("Updated product id={}", id);
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public void deactivate(long id) {
        Product product = require(id);
        product.deactivate();
        log.info("Deactivated product id={}", id);
    }

    private Product require(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " was not found"));
    }

    private static ConflictException duplicateSku(String sku) {
        return new ConflictException("DUPLICATE_SKU", "A product with SKU " + sku + " already exists");
    }
    private static String normalizeSku(String value) { return value.trim().toUpperCase(); }
    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
