package com.example.project.customer.service;

import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getAddresses();
    AddressResponse getAddressById(Integer id);
    AddressResponse createAddress(AddressRequest request);
    AddressResponse updateAddress(Integer id, AddressRequest request);
    void deleteAddress(Integer id);
}
