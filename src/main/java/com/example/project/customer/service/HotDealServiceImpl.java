package com.example.project.customer.service;

import com.example.project.customer.dto.HomeHotDealsResponse;
import com.example.project.customer.dto.HotDealReorderItem;
import com.example.project.customer.dto.HotDealReorderRequest;
import com.example.project.customer.dto.HotDealRequest;
import com.example.project.customer.dto.HotDealResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.HotDeal;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.HotDealRepository;
import com.example.project.customer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HotDealServiceImpl implements HotDealService {

    private final HotDealRepository hotDealRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ProductAvailabilityService productAvailabilityService;

    public void setProductAvailabilityService(ProductAvailabilityService productAvailabilityService) {
        this.productAvailabilityService = productAvailabilityService;
    }

    @Override
    public HotDealResponse addHotDeal(HotDealRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required to create a Hot Deal");
        }

        Integer productId = request.getProductId();

        // 1. Verify product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getApprovalStatus() != ApprovalStatus.APPROVED || !Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException("Cannot create a Hot Deal for an unapproved or inactive product.");
        }

        // 2. Prevent duplicate product in Hot Deals
        if (hotDealRepository.existsByProduct_ProductId(productId)) {
            throw new ResourceConflictException("Product ID " + productId + " already exists in Hot Deals");
        }

        // 3. Resolve display order
        int displayOrder = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : hotDealRepository.findMaxDisplayOrder() + 1;

        boolean active = request.isActive() != null ? request.isActive() : true;

        HotDeal hotDeal = HotDeal.builder()
                .product(product)
                .displayOrder(displayOrder)
                .active(active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        applyFlexibleDiscount(hotDeal, product, request.getDiscountPercentage(), request.getDealPrice());

        HotDeal saved = hotDealRepository.save(hotDeal);
        log.info("Added product '{}' (ID: {}) to Hot Deals at display order {} with discountPct: {}, dealPrice: {}",
                product.getTitle(), productId, displayOrder, saved.getDiscountPercentage(), saved.getDealPrice());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotDealResponse> getAdminHotDeals() {
        return hotDealRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotDealResponse getById(Long id) {
        HotDeal deal = findHotDeal(id);
        return mapToResponse(deal);
    }

    @Override
    public HotDealResponse updateHotDeal(Long id, HotDealRequest request) {
        HotDeal existing = findHotDeal(id);

        if (request.getProductId() != null && !request.getProductId().equals(existing.getProduct().getProductId())) {
            Integer newProductId = request.getProductId();
            Product newProduct = productRepository.findById(newProductId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + newProductId));

            if (newProduct.getApprovalStatus() != ApprovalStatus.APPROVED || !Boolean.TRUE.equals(newProduct.getActive())) {
                throw new IllegalArgumentException("Cannot update Hot Deal with an unapproved or inactive product.");
            }

            if (hotDealRepository.existsByProduct_ProductId(newProductId)) {
                throw new ResourceConflictException("Product ID " + newProductId + " already exists in Hot Deals");
            }
            existing.setProduct(newProduct);
        }

        if (request.getDisplayOrder() != null) {
            existing.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.isActive() != null) {
            existing.setActive(request.isActive());
        }

        if (request.getDiscountPercentage() != null || request.getDealPrice() != null) {
            applyFlexibleDiscount(existing, existing.getProduct(), request.getDiscountPercentage(), request.getDealPrice());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        HotDeal updated = hotDealRepository.save(existing);
        return mapToResponse(updated);
    }

    @Override
    public HotDealResponse setStatus(Long id, boolean active) {
        HotDeal deal = findHotDeal(id);
        deal.setActive(active);
        deal.setUpdatedAt(LocalDateTime.now());
        HotDeal updated = hotDealRepository.save(deal);
        log.info("Updated Hot Deal ID {} active status to {}", id, active);
        return mapToResponse(updated);
    }

    @Override
    public void deleteHotDeal(Long id) {
        HotDeal deal = findHotDeal(id);
        hotDealRepository.delete(deal);
        log.info("Removed Hot Deal ID {} (Product: {}). Product was NOT deleted.", id, deal.getProduct().getProductId());
    }

    @Override
    public List<HotDealResponse> reorderHotDeals(HotDealReorderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Reorder items cannot be empty");
        }

        Map<Long, Integer> orderMap = request.getItems().stream()
                .collect(Collectors.toMap(HotDealReorderItem::getId, HotDealReorderItem::getDisplayOrder, (a, b) -> b));

        List<HotDeal> allDeals = hotDealRepository.findAll();
        List<HotDeal> toUpdate = new ArrayList<>();

        for (HotDeal deal : allDeals) {
            if (orderMap.containsKey(deal.getId())) {
                deal.setDisplayOrder(orderMap.get(deal.getId()));
                deal.setUpdatedAt(LocalDateTime.now());
                toUpdate.add(deal);
            }
        }

        hotDealRepository.saveAll(toUpdate);

        return hotDealRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HomeHotDealsResponse getActiveHotDealsForHome() {
        List<HotDeal> activeDeals = hotDealRepository.findByActiveTrueOrderByDisplayOrderAsc();

        List<Product> productsToBatch = activeDeals.stream()
                .map(HotDeal::getProduct)
                .filter(p -> p != null && Boolean.TRUE.equals(p.getActive()) && p.getApprovalStatus() == ApprovalStatus.APPROVED)
                .toList();

        Map<Integer, com.example.project.customer.dto.ProductAvailability> availMap = (productAvailabilityService != null)
                ? productAvailabilityService.getAvailabilityBatch(productsToBatch)
                : java.util.Collections.emptyMap();

        List<ProductResponse> products = new ArrayList<>();
        for (HotDeal deal : activeDeals) {
            Product p = deal.getProduct();
            // Ensure product is approved and active for public customer visibility
            if (Boolean.TRUE.equals(p.getActive()) && p.getApprovalStatus() == ApprovalStatus.APPROVED) {
                com.example.project.customer.dto.ProductAvailability avail = (p != null) ? availMap.get(p.getProductId()) : null;
                ProductResponse resp = enrichProductWithDeal(p, deal, avail);
                products.add(resp);
            }
        }

        return HomeHotDealsResponse.builder()
                .section("Hot Deals")
                .products(products)
                .total(products.size())
                .build();
    }

    private void applyFlexibleDiscount(HotDeal deal, Product product, BigDecimal requestedDiscountPct, BigDecimal requestedDealPrice) {
        BigDecimal basePrice = product.getPrice();
        BigDecimal finalDiscountPct = requestedDiscountPct;
        BigDecimal finalDealPrice = requestedDealPrice;

        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
            if (finalDiscountPct != null && finalDealPrice == null) {
                // Admin provided discount % -> flexibly calculate deal price
                BigDecimal discountAmount = basePrice.multiply(finalDiscountPct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                finalDealPrice = basePrice.subtract(discountAmount);
            } else if (finalDealPrice != null && finalDiscountPct == null) {
                // Admin provided deal price -> flexibly calculate discount %
                if (finalDealPrice.compareTo(basePrice) < 0) {
                    finalDiscountPct = basePrice.subtract(finalDealPrice)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(basePrice, 2, RoundingMode.HALF_UP);
                }
            }
        }

        deal.setDiscountPercentage(finalDiscountPct);
        deal.setDealPrice(finalDealPrice);
    }

    private ProductResponse enrichProductWithDeal(Product p, HotDeal deal) {
        return enrichProductWithDeal(p, deal, null);
    }

    private ProductResponse enrichProductWithDeal(Product p, HotDeal deal, com.example.project.customer.dto.ProductAvailability providedAvail) {
        ProductResponse resp = productService.mapToResponse(p);
        com.example.project.customer.dto.ProductAvailability avail = providedAvail;
        if (avail == null && productAvailabilityService != null && p != null) {
            avail = productAvailabilityService.getAvailability(p);
        }
        if (avail != null) {
            resp.setStockQty(avail.getAvailableQty());
            resp.setAvailableQty(avail.getAvailableQty());
            resp.setInStock(avail.isInStock());
            resp.setAvailabilityReason(avail.getReason());
        }
        resp.setDisplayOrder(deal.getDisplayOrder());

        if (deal.getDiscountPercentage() != null) {
            resp.setDiscountPercentage(deal.getDiscountPercentage());
            if (deal.getDealPrice() != null) {
                resp.setDealPrice(deal.getDealPrice());
                resp.setSellingPrice(deal.getDealPrice());
            }
        } else if (p.getMrp() != null && p.getPrice() != null && p.getMrp().compareTo(p.getPrice()) > 0) {
            // Flexible fallback: calculate authentic discount from actual MRP if available
            BigDecimal diff = p.getMrp().subtract(p.getPrice());
            BigDecimal calcPct = diff.multiply(BigDecimal.valueOf(100))
                    .divide(p.getMrp(), 2, RoundingMode.HALF_UP);
            resp.setDiscountPercentage(calcPct);
        }
        return resp;
    }

    private HotDeal findHotDeal(Long id) {
        return hotDealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hot Deal not found with id: " + id));
    }

    private HotDealResponse mapToResponse(HotDeal hotDeal) {
        ProductResponse productResp = enrichProductWithDeal(hotDeal.getProduct(), hotDeal);

        return HotDealResponse.builder()
                .id(hotDeal.getId())
                .productId(hotDeal.getProduct().getProductId())
                .displayOrder(hotDeal.getDisplayOrder())
                .discountPercentage(hotDeal.getDiscountPercentage())
                .dealPrice(hotDeal.getDealPrice())
                .active(hotDeal.isActive())
                .createdAt(hotDeal.getCreatedAt())
                .updatedAt(hotDeal.getUpdatedAt())
                .product(productResp)
                .build();
    }
}
