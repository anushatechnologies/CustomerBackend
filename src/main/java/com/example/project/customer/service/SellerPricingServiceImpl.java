package com.example.project.customer.service;

import com.example.project.customer.dto.BulkPriceAdjustmentRequest;
import com.example.project.customer.entity.Product;
import com.example.project.customer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerPricingServiceImpl implements SellerPricingService {

    private final ProductRepository productRepository;

    @Override
    public Map<String, Object> bulkAdjustPricing(Integer sellerId, BulkPriceAdjustmentRequest request) {
        List<Product> products = productRepository.findBySellerId(sellerId);

        // Filter by category or brand if specified
        if (request.getCategoryId() != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && request.getCategoryId().equals(p.getCategory().getCategoryId()))
                    .toList();
        }

        if (request.getBrandId() != null) {
            products = products.stream()
                    .filter(p -> p.getBrand() != null && request.getBrandId().equals(p.getBrand().getBrandId()))
                    .toList();
        } else if (request.getBrand() != null && !request.getBrand().isBlank()) {
            String bName = request.getBrand().trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getBrand() != null && p.getBrand().getName().toLowerCase().contains(bName))
                    .toList();
        }

        int modifiedCount = 0;
        String type = request.getAdjustmentType() != null ? request.getAdjustmentType().trim().toLowerCase() : "percentage_increase";
        String applyTo = request.getApplyTo() != null ? request.getApplyTo().trim().toLowerCase() : "both";
        BigDecimal value = request.getValue() != null ? request.getValue() : BigDecimal.ZERO;

        for (Product p : products) {
            boolean priceChanged = false;

            // Adjust Selling Price / Price
            if ("selling_price".equals(applyTo) || "sellingprice".equals(applyTo) || "both".equals(applyTo)) {
                BigDecimal current = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                BigDecimal updated = calculateAdjustedPrice(current, type, value);
                p.setPrice(updated);
                p.setSellingPrice(updated);
                priceChanged = true;
            }

            // Adjust MRP
            if ("mrp".equals(applyTo) || "both".equals(applyTo)) {
                BigDecimal currentMrp = p.getMrp() != null ? p.getMrp() : p.getPrice();
                if (currentMrp != null) {
                    BigDecimal updatedMrp = calculateAdjustedPrice(currentMrp, type, value);
                    p.setMrp(updatedMrp);
                    priceChanged = true;
                }
            }

            if (priceChanged) {
                p.setUpdatedAt(LocalDateTime.now());
                productRepository.save(p);
                modifiedCount++;
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Bulk price adjustment applied to " + modifiedCount + " products");
        response.put("modifiedCount", modifiedCount);
        return response;
    }

    private BigDecimal calculateAdjustedPrice(BigDecimal base, String type, BigDecimal val) {
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return base != null ? base : BigDecimal.ZERO;
        }

        BigDecimal result;
        switch (type) {
            case "percentage_increase":
            case "percentage-increase":
            case "percent_increase":
                result = base.add(base.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                break;
            case "percentage_decrease":
            case "percentage-decrease":
            case "percent_decrease":
                result = base.subtract(base.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                break;
            case "fixed_increase":
            case "fixed-increase":
                result = base.add(val);
                break;
            case "fixed_decrease":
            case "fixed-decrease":
                result = base.subtract(val);
                break;
            default:
                result = base;
                break;
        }
        return result.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
