package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> getAddresses(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(addressService.getAddresses(uid));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressResponse> getAddressById(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(addressService.getAddressById(uid, id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Valid @RequestBody AddressRequest request) {
        Integer uid = userId != null ? userId : 101;
        AddressResponse response = addressService.addAddress(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Address added successfully", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id,
            @Valid @RequestBody AddressRequest request) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok("Address updated successfully", addressService.updateAddress(uid, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        addressService.deleteAddress(uid, id);
        return ResponseEntity.noContent().build();
    }
}
