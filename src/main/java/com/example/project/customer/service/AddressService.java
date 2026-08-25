package com.example.project.customer.service;

import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getAddresses(Integer userId);
    AddressResponse getAddressById(Integer userId, Integer addressId);
    AddressResponse addAddress(Integer userId, AddressRequest request);
    AddressResponse updateAddress(Integer userId, Integer addressId, AddressRequest request);
    void deleteAddress(Integer userId, Integer addressId);
}
