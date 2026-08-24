package com.example.project.customer.service;

import com.example.project.customer.dto.CustomerRequest;
import com.example.project.customer.dto.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(Integer id);

    List<CustomerResponse> getAllCustomers();

    CustomerResponse updateCustomer(Integer id, CustomerRequest request);

    void deleteCustomer(Integer id);
}
