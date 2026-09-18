package com.example.project.customer.service;

import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class CouponServiceImpl implements CouponService {
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final OrderRepository orderRepository;

    @Override @Transactional(readOnly = true)
    public CouponValidationResult validateAndCalculateDiscount(String code, Customer customer, BigDecimal cartSubtotal) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Coupon code is required");
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim()).orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        validate(coupon, customer, amount(cartSubtotal), false);
        return new CouponValidationResult(coupon, calculate(coupon, amount(cartSubtotal)));
    }

    @Override @Transactional(readOnly = true)
    public List<EligibleCouponResponse> getEligibleCouponsForCart(Customer customer, BigDecimal cartSubtotal) {
        BigDecimal subtotal = amount(cartSubtotal);
        return couponRepository.findByIsActiveTrue().stream().map(coupon -> {
            BigDecimal shortfall = coupon.getMinOrderAmount().subtract(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            boolean applicable;
            BigDecimal discount = BigDecimal.ZERO;
            try { validate(coupon, customer, subtotal, false); applicable = true; discount = calculate(coupon, subtotal); }
            catch (RuntimeException ignored) { applicable = false; }
            return EligibleCouponResponse.builder().code(coupon.getCode()).title(coupon.getTitle()).description(coupon.getDescription())
                    .discountType(coupon.getDiscountType()).discountValue(coupon.getDiscountValue()).minOrderAmount(coupon.getMinOrderAmount())
                    .maxDiscountAmount(coupon.getMaxDiscountAmount()).expiryDate(coupon.getExpiryDate()).isApplicable(applicable)
                    .estimatedDiscount(discount).shortfallAmount(shortfall).build();
        }).toList();
    }

    @Override
    public void recordCouponUsage(String code, Customer customer, Order order, BigDecimal discount) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Coupon code is required");
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        // The order is already persisted to obtain its foreign-key id. Do not count that
        // just-created order against a first-order coupon.
        validate(coupon, customer, amount(order.getSubtotal()), true);
        CouponValidationResult result = new CouponValidationResult(coupon, calculate(coupon, amount(order.getSubtotal())));
        if (couponRepository.incrementUsageIfAvailable(result.getCoupon().getId()) == 0) throw new ResourceConflictException("Coupon usage limit has been reached");
        couponUsageRepository.save(CouponUsage.builder().coupon(result.getCoupon()).customer(customer).order(order).discountApplied(result.getDiscount()).build());
    }

    private void validate(Coupon c, Customer customer, BigDecimal subtotal, boolean orderAlreadyPersisted) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(c.getIsActive())) throw new IllegalStateException("Coupon is inactive");
        if (c.getStartDate() != null && now.isBefore(c.getStartDate())) throw new IllegalStateException("Coupon is not active yet");
        if (c.getExpiryDate() != null && now.isAfter(c.getExpiryDate())) throw new IllegalStateException("Coupon has expired");
        if (subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            BigDecimal shortfall = c.getMinOrderAmount().subtract(subtotal).setScale(2, RoundingMode.HALF_UP);
            throw new IllegalStateException("Add \u20B9" + shortfall + " more to avail this coupon. Minimum order amount is \u20B9" + c.getMinOrderAmount() + ".");
        }
        if (c.getTotalUsageLimit() != null && c.getUsedCount() >= c.getTotalUsageLimit()) throw new IllegalStateException("Coupon usage limit has been reached");
        if (couponUsageRepository.countByCouponAndCustomer(c, customer) >= c.getPerUserLimit()) throw new IllegalStateException("You have already redeemed coupon " + c.getCode() + " the maximum allowed number of times.");
        long priorOrderCount = orderRepository.countNonCancelledByCustomerId(customer.getCustomerId());
        if (Boolean.TRUE.equals(c.getFirstOrderOnly()) && priorOrderCount > (orderAlreadyPersisted ? 1 : 0)) throw new IllegalStateException("This coupon is only valid on your first order");
    }
    private BigDecimal calculate(Coupon c, BigDecimal subtotal) {
        BigDecimal discount = c.getDiscountType() == DiscountType.PERCENTAGE ? subtotal.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : c.getDiscountValue();
        if (c.getDiscountType() == DiscountType.PERCENTAGE && c.getMaxDiscountAmount() != null) discount = discount.min(c.getMaxDiscountAmount());
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    @Override public CouponAdminResponse create(CouponAdminRequest request) {
        String code = request.getCode().trim();
        if (couponRepository.existsByCodeIgnoreCase(code)) throw new ResourceConflictException("Coupon code already exists");
        Coupon coupon = Coupon.builder().build(); apply(coupon, request); return response(couponRepository.save(coupon));
    }
    @Override @Transactional(readOnly = true) public CouponAdminResponse getById(Long id) { return response(find(id)); }
    @Override public CouponAdminResponse update(Long id, CouponAdminRequest request) {
        Coupon coupon = find(id); if (!coupon.getCode().equalsIgnoreCase(request.getCode()) && couponRepository.existsByCodeIgnoreCase(request.getCode())) throw new ResourceConflictException("Coupon code already exists");
        apply(coupon, request); return response(couponRepository.save(coupon));
    }
    @Override public CouponAdminResponse toggleStatus(Long id, boolean isActive) { Coupon c = find(id); c.setIsActive(isActive); return response(couponRepository.save(c)); }
    @Override public void delete(Long id) {
        Coupon coupon = find(id);
        if (couponUsageRepository.countByCoupon(coupon) > 0) {
            throw new ResourceConflictException("A redeemed coupon cannot be deleted; deactivate it instead to preserve the audit trail");
        }
        couponRepository.delete(coupon);
    }
    @Override @Transactional(readOnly = true) public Page<CouponAdminResponse> list(Boolean active, String search, int page, int size) { return couponRepository.search(active, blankToNull(search), PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("createdAt").descending())).map(this::response); }
    @Override @Transactional(readOnly = true) public Page<CouponUsageResponse> usages(Long id, int page, int size) { return couponUsageRepository.findByCouponOrderByUsedAtDesc(find(id), PageRequest.of(Math.max(0, page), Math.max(1, size))).map(u -> CouponUsageResponse.builder().id(u.getId()).customerId(u.getCustomer().getCustomerId()).customerName(u.getCustomer().getName()).orderId(u.getOrder().getOrderId()).orderNumber(u.getOrder().getOrderNumber()).discountApplied(u.getDiscountApplied()).usedAt(u.getUsedAt()).build()); }
    private Coupon find(Long id) { return couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id)); }
    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private void apply(Coupon c, CouponAdminRequest r) {
        if (r.getDiscountType() == DiscountType.PERCENTAGE && r.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        if (r.getDiscountType() == DiscountType.FIXED_AMOUNT && r.getMaxDiscountAmount() != null) throw new IllegalArgumentException("Maximum discount amount is only valid for percentage coupons");
        if (r.getStartDate() != null && r.getExpiryDate() != null && r.getExpiryDate().isBefore(r.getStartDate())) throw new IllegalArgumentException("Expiry date must be after start date");
        c.setCode(r.getCode()); c.setTitle(r.getTitle()); c.setDescription(r.getDescription()); c.setDiscountType(r.getDiscountType()); c.setDiscountValue(r.getDiscountValue()); c.setMinOrderAmount(r.getMinOrderAmount() == null ? BigDecimal.ZERO : r.getMinOrderAmount()); c.setMaxDiscountAmount(r.getMaxDiscountAmount()); c.setStartDate(r.getStartDate()); c.setExpiryDate(r.getExpiryDate()); c.setTotalUsageLimit(r.getTotalUsageLimit()); c.setPerUserLimit(r.getPerUserLimit() == null ? 1 : r.getPerUserLimit()); c.setFirstOrderOnly(Boolean.TRUE.equals(r.getFirstOrderOnly())); c.setIsActive(r.getIsActive() == null || r.getIsActive());
    }
    private CouponAdminResponse response(Coupon c) { return CouponAdminResponse.builder().id(c.getId()).code(c.getCode()).title(c.getTitle()).description(c.getDescription()).discountType(c.getDiscountType()).discountValue(c.getDiscountValue()).minOrderAmount(c.getMinOrderAmount()).maxDiscountAmount(c.getMaxDiscountAmount()).startDate(c.getStartDate()).expiryDate(c.getExpiryDate()).totalUsageLimit(c.getTotalUsageLimit()).usedCount(c.getUsedCount()).perUserLimit(c.getPerUserLimit()).firstOrderOnly(c.getFirstOrderOnly()).isActive(c.getIsActive()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build(); }
}
