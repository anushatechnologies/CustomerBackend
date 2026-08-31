package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CouponApplyRequest;
import com.example.project.customer.dto.CouponApplyResponse;
import com.example.project.customer.dto.CouponRequest;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.dto.CouponValidationRequest;
import com.example.project.customer.dto.CouponValidationResponse;
import com.example.project.customer.service.CouponExpiryService;
import com.example.project.customer.service.CouponManagementService;
import com.example.project.customer.service.CouponUsageService;
import com.example.project.customer.service.CouponValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponManagementService couponManagementService;
    private final CouponValidationService couponValidationService;
    private final CouponUsageService couponUsageService;
    private final CouponExpiryService couponExpiryService;

    // ============================================
    // ADMIN ENDPOINTS
    // ============================================

    /**
     * Create a new coupon (Admin only).
     *
     * @param request the coupon creation request
     * @return the created coupon
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CouponRequest request) {
        log.info("Admin creating coupon with code: {}", request.getCode());

        try {
            CouponResponse created = couponManagementService.createCoupon(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("Coupon created successfully", created));
        } catch (IllegalArgumentException e) {
            log.warn("Error creating coupon: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating coupon: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to create coupon"));
        }
    }

    /**
     * Update an existing coupon (Admin only).
     *
     * @param couponId the coupon ID
     * @param request the coupon update request
     * @return the updated coupon
     */
    @PutMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Integer couponId,
            @Valid @RequestBody CouponRequest request) {
        log.info("Admin updating coupon with ID: {}", couponId);

        try {
            CouponResponse updated = couponManagementService.updateCoupon(couponId, request);
            return ResponseEntity.ok(ApiResponse.ok("Coupon updated successfully", updated));
        } catch (IllegalArgumentException e) {
            log.warn("Error updating coupon: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating coupon: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to update coupon"));
        }
    }

    /**
     * Get a coupon by ID (Admin).
     *
     * @param couponId the coupon ID
     * @return the coupon details
     */
    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(
            @PathVariable Integer couponId) {
        log.debug("Fetching coupon by ID: {}", couponId);

        return couponManagementService.getCouponById(couponId)
                .map(coupon -> ResponseEntity.ok(ApiResponse.ok("Coupon retrieved successfully", coupon)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Coupon not found")));
    }

    /**
     * List all coupons (Admin).
     *
     * @return list of all coupons
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> listAllCoupons() {
        log.debug("Listing all coupons");

        List<CouponResponse> coupons = couponManagementService.listAllCoupons();
        return ResponseEntity.ok(ApiResponse.ok("Coupons retrieved successfully", coupons));
    }

    /**
     * Activate a coupon (Admin only).
     *
     * @param couponId the coupon ID
     * @return the updated coupon
     */
    @PatchMapping("/{couponId}/activate")
    public ResponseEntity<ApiResponse<CouponResponse>> activateCoupon(
            @PathVariable Integer couponId) {
        log.info("Admin activating coupon with ID: {}", couponId);

        try {
            CouponResponse updated = couponManagementService.activateCoupon(couponId);
            return ResponseEntity.ok(ApiResponse.ok("Coupon activated successfully", updated));
        } catch (IllegalArgumentException e) {
            log.warn("Error activating coupon: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * Deactivate a coupon (Admin only).
     *
     * @param couponId the coupon ID
     * @return the updated coupon
     */
    @PatchMapping("/{couponId}/deactivate")
    public ResponseEntity<ApiResponse<CouponResponse>> deactivateCoupon(
            @PathVariable Integer couponId) {
        log.info("Admin deactivating coupon with ID: {}", couponId);

        try {
            CouponResponse updated = couponManagementService.deactivateCoupon(couponId);
            return ResponseEntity.ok(ApiResponse.ok("Coupon deactivated successfully", updated));
        } catch (IllegalArgumentException e) {
            log.warn("Error deactivating coupon: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * Delete a coupon (Admin only).
     *
     * @param couponId the coupon ID
     * @return success response
     */
    @DeleteMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(
            @PathVariable Integer couponId) {
        log.info("Admin deleting coupon with ID: {}", couponId);

        try {
            couponManagementService.deleteCoupon(couponId);
            return ResponseEntity.ok(ApiResponse.ok("Coupon deleted successfully", null));
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting coupon: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    /**
     * Trigger expiry check manually (Admin only).
     *
     * @return success response
     */
    @PostMapping("/admin/trigger-expiry")
    public ResponseEntity<ApiResponse<Void>> triggerExpiryCheck() {
        log.info("Admin triggering manual expiry check");

        couponExpiryService.triggerExpiryCheck();
        return ResponseEntity.ok(ApiResponse.ok("Expiry check triggered successfully", null));
    }

    // ============================================
    // CUSTOMER ENDPOINTS
    // ============================================

    /**
     * Validate a coupon code without consuming usage (Customer).
     * This is used for preview/discount calculation before final order confirmation.
     *
     * @param request the validation request with coupon code and order details
     * @param userId the user ID
     * @return validation result with discount amount if valid
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody CouponValidationRequest request,
            @RequestParam Integer userId) {
        log.info("Customer {} validating coupon: {}", userId, request.getCouponCode());

        CouponValidationResponse response = couponValidationService.validateCoupon(request, userId);

        if (response.isValid()) {
            return ResponseEntity.ok(ApiResponse.ok("Coupon is valid", response));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Coupon validation failed", response));
        }
    }

    /**
     * Apply a coupon to an order (Customer).
     * This validates the coupon and atomically consumes its usage.
     * Call this at order confirmation time, not at preview time.
     *
     * @param request the apply request with coupon code and order details
     * @return apply result with discount amount if successful
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CouponApplyResponse>> applyCoupon(
            @Valid @RequestBody CouponApplyRequest request) {
        log.info("Customer {} applying coupon to order: {}", request.getUserId(), request.getCouponCode());

        CouponApplyResponse response = couponUsageService.applyCoupon(
                request.getCouponCode(),
                request.getUserId(),
                request.getOrderId(),
                request.getOrderFare(),
                request.getVehicleType()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.ok("Coupon applied successfully", response));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Failed to apply coupon", response));
        }
    }

    /**
     * Release a coupon when an order is cancelled (Customer/System).
     * Refunds the usage slot back to the coupon.
     *
     * @param orderId the order ID
     * @return success response
     */
    @PostMapping("/orders/{orderId}/release")
    public ResponseEntity<ApiResponse<Void>> releaseCouponForOrder(
            @PathVariable Integer orderId) {
        log.info("Releasing coupon for cancelled order: {}", orderId);

        couponUsageService.releaseCouponUsage(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Coupon usage released successfully", null));
    }

    /**
     * Get a coupon by code (Customer).
     *
     * @param code the coupon code
     * @return the coupon details
     */
    @GetMapping("/by-code")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponByCode(
            @RequestParam String code) {
        log.debug("Fetching coupon by code: {}", code);

        return couponManagementService.getCouponByCode(code)
                .map(coupon -> ResponseEntity.ok(ApiResponse.ok("Coupon retrieved successfully", coupon)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "Coupon not found")));
    }
}
