package com.example.orders.product;

import com.example.orders.product.dto.CreateProductRequest;
import com.example.orders.shared.error.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    @Test
    void rejectsDuplicateSkuIgnoringCase() {
        ProductRepository repository = mock(ProductRepository.class);
        when(repository.existsBySkuIgnoreCase("ABC-1")).thenReturn(true);
        ProductService service = new ProductService(repository);

        assertThatThrownBy(() -> service.create(new CreateProductRequest(
                "abc-1", "Product", null, new BigDecimal("10.00"), 1)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("DUPLICATE_SKU");
        verify(repository, never()).save(any());
    }
}
