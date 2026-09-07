package com.example.project.customer.service;

import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CustomerRepository customerRepository;

    private AddressServiceImpl addressService;

    private Customer customer101;
    private Customer customer202;

    @BeforeEach
    void setUp() {
        addressService = new AddressServiceImpl(addressRepository, customerRepository);
        customer101 = Customer.builder().customerId(101).name("Customer 101").phone("9876543210").build();
        customer202 = Customer.builder().customerId(202).name("Customer 202").phone("9123456780").build();
    }

    @Test
    @DisplayName("AddressService: First address created for a customer automatically becomes default")
    void testCreateAddress_FirstAddress_AutomaticallyDefault() {
        when(addressRepository.findByCustomer_CustomerIdOrderByIsDefaultDescCreatedAtDesc(101))
                .thenReturn(new ArrayList<>());
        when(customerRepository.findById(101)).thenReturn(Optional.of(customer101));

        Address saved = Address.builder()
                .id(1)
                .customer(customer101)
                .siteName("Primary Site")
                .recipientName("Customer 101")
                .phone("9876543210")
                .addressLine1("Line 1")
                .city("Hyderabad")
                .state("Telangana")
                .country("India")
                .pincode("500001")
                .isDefault(true)
                .build();

        when(addressRepository.save(any(Address.class))).thenReturn(saved);

        AddressRequest request = AddressRequest.builder()
                .addressLine1("Line 1")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500001")
                .isDefault(false) // Even if requested false, first address becomes default
                .build();

        AddressResponse response = addressService.createAddress(101, request);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("AddressService: New default address unsets previous default address")
    void testCreateAddress_NewDefault_UnsetsPreviousDefault() {
        Address oldDefault = Address.builder()
                .id(1)
                .customer(customer101)
                .isDefault(true)
                .build();

        when(addressRepository.findByCustomer_CustomerIdOrderByIsDefaultDescCreatedAtDesc(101))
                .thenReturn(List.of(oldDefault));
        when(addressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(101))
                .thenReturn(Optional.of(oldDefault));
        when(customerRepository.findById(101)).thenReturn(Optional.of(customer101));

        Address newDefault = Address.builder()
                .id(2)
                .customer(customer101)
                .isDefault(true)
                .build();
        when(addressRepository.save(any(Address.class))).thenReturn(newDefault);

        AddressRequest request = AddressRequest.builder()
                .addressLine1("Line 2")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .isDefault(true)
                .build();

        addressService.createAddress(101, request);

        assertThat(oldDefault.getIsDefault()).isFalse();
        verify(addressRepository, times(2)).save(any(Address.class));
    }

    @Test
    @DisplayName("AddressService: Setting default address toggles previous default off")
    void testSetDefaultAddress_Success() {
        Address addr1 = Address.builder().id(1).customer(customer101).isDefault(true).build();
        Address addr2 = Address.builder().id(2).customer(customer101).isDefault(false).build();

        when(addressRepository.findByCustomer_CustomerIdAndId(101, 2)).thenReturn(Optional.of(addr2));
        when(addressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(101)).thenReturn(Optional.of(addr1));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressResponse response = addressService.setDefaultAddress(101, 2);

        assertThat(addr1.getIsDefault()).isFalse();
        assertThat(addr2.getIsDefault()).isTrue();
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    @DisplayName("AddressService: Deleting default address promotes most recent remaining address")
    void testDeleteAddress_PromotesNextDefault() {
        Address defaultAddr = Address.builder().id(1).customer(customer101).isDefault(true).build();
        Address remainingAddr = Address.builder().id(2).customer(customer101).isDefault(false).build();

        when(addressRepository.findByCustomer_CustomerIdAndId(101, 1)).thenReturn(Optional.of(defaultAddr));
        when(addressRepository.findFirstByCustomer_CustomerIdOrderByCreatedAtDesc(101))
                .thenReturn(Optional.of(remainingAddr));

        addressService.deleteAddress(101, 1);

        verify(addressRepository).delete(defaultAddr);
        assertThat(remainingAddr.getIsDefault()).isTrue();
        verify(addressRepository).save(remainingAddr);
    }

    @Test
    @DisplayName("Customer Isolation: Customer 101 cannot access or modify Customer 202's address")
    void testCustomerIsolation_OwnershipEnforced() {
        when(addressRepository.findByCustomer_CustomerIdAndId(101, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddressById(101, 99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found with id: 99");

        assertThatThrownBy(() -> addressService.deleteAddress(101, 99))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> addressService.setDefaultAddress(101, 99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).delete(any());
    }
}
