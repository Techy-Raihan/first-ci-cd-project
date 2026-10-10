package com.example.orders.customer;

import com.example.orders.customer.dto.CustomerRequest;
import com.example.orders.shared.error.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CustomerServiceTest {
    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        CustomerRepository repository = mock(CustomerRepository.class);
        when(repository.existsByEmailIgnoreCase("ravi@example.com")).thenReturn(true);
        CustomerService service = new CustomerService(repository);

        assertThatThrownBy(() -> service.create(new CustomerRequest(
                "Ravi", "RAVI@example.com", "9999999999", "Pune")))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo("DUPLICATE_EMAIL");
        verify(repository, never()).save(any());
    }
}
