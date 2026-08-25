package com.example.project.customer.service;

import com.example.project.customer.dto.CustomerRequest;
import com.example.project.customer.dto.CustomerResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.exception.CustomerConflictException;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");
    }

    @Test
    @DisplayName("Create customer - Success")
    void createCustomer_Success() {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");

        when(customerRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Create customer - Duplicate email conflict")
    void createCustomer_Conflict() {
        CustomerRequest request = new CustomerRequest();
        request.setEmail("john@example.com");

        when(customerRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(CustomerConflictException.class);
    }

    @Test
    @DisplayName("Get customer by ID - Success")
    void getCustomerById_Success() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerById(1);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get customer by ID - Not Found")
    void getCustomerById_NotFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    @DisplayName("Get all customers - Success")
    void getAllCustomers_Success() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("Update customer - Success")
    void updateCustomer_Success() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Updated");
        request.setEmail("updated@example.com");
        request.setPhone("1234567890");

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot("updated@example.com", 1)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.updateCustomer(1, request);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Delete customer - Success")
    void deleteCustomer_Success() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1);

        verify(customerRepository).delete(customer);
    }
}
