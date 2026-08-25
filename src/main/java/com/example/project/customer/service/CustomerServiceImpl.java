package com.example.project.customer.service;

import com.example.project.customer.dto.CustomerRequest;
import com.example.project.customer.dto.CustomerResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.exception.CustomerConflictException;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new CustomerConflictException("Customer already exists with email: " + request.getEmail());
        }
        Customer customer = new Customer();
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer id) {
        return toResponse(findCustomer(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public CustomerResponse updateCustomer(Integer id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        if (customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot(request.getEmail(), id)) {
            throw new CustomerConflictException("Customer already exists with email: " + request.getEmail());
        }
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Override
    public void deleteCustomer(Integer id) {
        customerRepository.delete(findCustomer(id));
    }

    private Customer findCustomer(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getCustomerId(), customer.getName(), customer.getEmail(), customer.getPhone());
    }
}
