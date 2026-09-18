package com.example.project.customer.controller;

import com.example.project.customer.dto.*;
import com.example.project.customer.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
// TODO: Re-enable before deploying to production
// @PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {
    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<ApiResponse<CouponAdminResponse>> create(@Valid @RequestBody CouponAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Coupon created successfully", couponService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponAdminResponse>>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Coupons retrieved successfully", couponService.list(active, search, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Coupon retrieved successfully", couponService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponAdminResponse>> update(@PathVariable Long id, @Valid @RequestBody CouponAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Coupon updated successfully", couponService.update(id, request)));
    }

    @PatchMapping({"/{id}/status", "/{id}/toggle-status"})
    public ResponseEntity<ApiResponse<CouponAdminResponse>> setStatus(@PathVariable Long id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) java.util.Map<String, Boolean> body) {
        if (active == null && (body == null || !body.containsKey("isActive"))) {
            throw new IllegalArgumentException("active query parameter or isActive request body field is required");
        }
        boolean resolvedActive = active != null ? active : Boolean.TRUE.equals(body.get("isActive"));
        return ResponseEntity.ok(ApiResponse.ok("Coupon status updated successfully", couponService.toggleStatus(id, resolvedActive)));
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<ApiResponse<Page<CouponUsageResponse>>> usages(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Coupon usages retrieved successfully", couponService.usages(id, page, size)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Coupon deleted successfully", null));
    }
}
