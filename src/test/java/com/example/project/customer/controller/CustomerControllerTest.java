package com.example.project.customer.controller;

import com.example.project.customer.dto.CustomerRequest;
import com.example.project.customer.dto.CustomerResponse;
import com.example.project.customer.exception.CustomerConflictException;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /api/customers - Success")
    void createCustomer_Success() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");

        CustomerResponse response = new CustomerResponse(1, "John", "john@example.com", "1234567890");

        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/customers - Conflict")
    void createCustomer_Conflict() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");

        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new CustomerConflictException("Email exists"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email exists"));
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Success")
    void getById_Success() throws Exception {
        when(customerService.getCustomerById(1))
                .thenReturn(new CustomerResponse(1, "John", "john@example.com", "1234567890"));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Not Found")
    void getById_NotFound() throws Exception {
        when(customerService.getCustomerById(99))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/customers - Success")
    void getAll_Success() throws Exception {
        when(customerService.getAllCustomers())
                .thenReturn(List.of(new CustomerResponse(1, "John", "john@example.com", "1234567890")));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - Success")
    void update_Success() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("John Updated");
        request.setEmail("john.updated@example.com");
        request.setPhone("1234567890");

        when(customerService.updateCustomer(eq(1), any(CustomerRequest.class)))
                .thenReturn(new CustomerResponse(1, "John Updated", "john.updated@example.com", "1234567890"));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"));
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - Success")
    void delete_Success() throws Exception {
        doNothing().when(customerService).deleteCustomer(1);

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(1);
    }
}
