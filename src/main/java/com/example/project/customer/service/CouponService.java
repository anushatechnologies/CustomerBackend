package com.example.project.customer.service;

import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    CouponValidationResult validateAndCalculateDiscount(String code, Customer customer, BigDecimal cartSubtotal);
    List<EligibleCouponResponse> getEligibleCouponsForCart(Customer customer, BigDecimal cartSubtotal);
    void recordCouponUsage(String code, Customer customer, Order order, BigDecimal discount);
    CouponAdminResponse create(CouponAdminRequest request);
    CouponAdminResponse getById(Long id);
    CouponAdminResponse update(Long id, CouponAdminRequest request);
    CouponAdminResponse toggleStatus(Long id, boolean isActive);
    void delete(Long id);
    Page<CouponAdminResponse> list(Boolean active, String search, int page, int size);
    Page<CouponUsageResponse> usages(Long id, int page, int size);
}
