package com.example.project.customer.service;

import com.example.project.customer.dto.CheckPhoneResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceCheckPhoneTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SellerRepository sellerRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(customerRepository, sellerRepository);
    }

    @Test
    @DisplayName("checkPhoneExists - Returns exists=true when customer exists in DB")
    void checkPhoneExists_CustomerFound() {
        Customer customer = Customer.builder()
                .customerId(1)
                .name("Alice Smith")
                .email("alice@example.com")
                .phone("+919876543210")
                .role("CUSTOMER")
                .build();

        when(customerRepository.findMatchingCustomersByPhone(anyString(), anyString(), anyString()))
                .thenReturn(List.of(customer));

        CheckPhoneResponse response = userService.checkPhoneExists("+919876543210");

        assertNotNull(response);
        assertTrue(response.isExists());
        assertEquals("+919876543210", response.getPhone());
        assertEquals("Alice Smith", response.getName());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    @DisplayName("checkPhoneExists - Returns exists=true with SELLER role when seller exists in DB")
    void checkPhoneExists_SellerFound() {
        Seller seller = Seller.builder()
                .sellerId(10)
                .name("Seller Corp")
                .email("seller@example.com")
                .phone("9876543210")
                .build();

        when(customerRepository.findMatchingCustomersByPhone(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(sellerRepository.findAllByPhone("9876543210"))
                .thenReturn(List.of(seller));

        CheckPhoneResponse response = userService.checkPhoneExists("9876543210");

        assertNotNull(response);
        assertTrue(response.isExists());
        assertEquals("9876543210", response.getPhone());
        assertEquals("Seller Corp", response.getName());
        assertEquals("SELLER", response.getRole());
    }

    @Test
    @DisplayName("checkPhoneExists - Returns exists=false when neither customer nor seller exists")
    void checkPhoneExists_NotFound() {
        when(customerRepository.findMatchingCustomersByPhone(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(sellerRepository.findAllByPhone(anyString()))
                .thenReturn(Collections.emptyList());

        CheckPhoneResponse response = userService.checkPhoneExists("9123456780");

        assertNotNull(response);
        assertFalse(response.isExists());
        assertEquals("9123456780", response.getPhone());
    }

    @Test
    @DisplayName("checkPhoneExists - Throws IllegalArgumentException when phone is null or blank")
    void checkPhoneExists_BlankPhone_Throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.checkPhoneExists(null));
        assertThrows(IllegalArgumentException.class, () -> userService.checkPhoneExists("   "));
    }
}
