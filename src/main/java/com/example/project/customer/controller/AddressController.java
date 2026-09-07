package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.AddressRequest;
import com.example.project.customer.dto.AddressResponse;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses() {
        Integer userId = userContextUtil.getCurrentUserId();
        List<AddressResponse> list = addressService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.ok("Addresses retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(@PathVariable Integer id) {
        Integer userId = userContextUtil.getCurrentUserId();
        AddressResponse address = addressService.getAddressById(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Address retrieved successfully", address));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        AddressResponse created = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Address created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Integer id,
            @Valid @RequestBody AddressRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        AddressResponse updated = addressService.updateAddress(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Integer id) {
        Integer userId = userContextUtil.getCurrentUserId();
        addressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully", null));
    }
}
