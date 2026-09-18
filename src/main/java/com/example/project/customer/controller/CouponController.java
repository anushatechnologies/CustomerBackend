package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.EligibleCouponResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CouponController {
    private final CouponService couponService;
    private final CartService cartService;
    private final CustomerRepository customerRepository;
    private final UserContextUtil userContextUtil;

    @GetMapping({"", "/eligible"})
    public ResponseEntity<ApiResponse<List<EligibleCouponResponse>>> eligibleForCurrentCart() {
        Integer customerId = userContextUtil.getCurrentUserId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        CartResponse cart = cartService.getCart(customerId);
        return ResponseEntity.ok(ApiResponse.ok("Eligible coupons retrieved successfully",
                couponService.getEligibleCouponsForCart(customer, cart.getSubtotal())));
    }
}
