package com.example.project.customer.service;

import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.WishlistResponse;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.WishlistItem;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(Integer userId) {
        int uid = userId != null ? userId : 101;
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(uid).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public WishlistResponse addToWishlist(Integer userId, Integer productId) {
        int uid = userId != null ? userId : 101;
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Optional<WishlistItem> existing = wishlistRepository.findByUserIdAndProduct_ProductId(uid, productId);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        WishlistItem item = WishlistItem.builder()
                .userId(uid)
                .product(product)
                .build();

        return mapToResponse(wishlistRepository.save(item));
    }

    @Override
    public void removeFromWishlist(Integer userId, Integer productId) {
        int uid = userId != null ? userId : 101;
        wishlistRepository.deleteByUserIdAndProduct_ProductId(uid, productId);
    }

    private WishlistResponse mapToResponse(WishlistItem item) {
        Product p = item.getProduct();
        Integer catId = (p.getSubcategory() != null && p.getSubcategory().getCategory() != null)
                ? p.getSubcategory().getCategory().getCategoryId() : null;

        ProductResponse pr = ProductResponse.builder()
                .productId(p.getProductId())
                .subcategoryId(p.getSubcategory() != null ? p.getSubcategory().getSubcategoryId() : null)
                .categoryId(catId)
                .title(p.getTitle())
                .slug(p.getSlug())
                .sku(p.getSku())
                .brand(p.getBrand())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .images(p.getImages())
                .price(p.getPrice())
                .mrp(p.getMrp())
                .unit(p.getUnit())
                .moq(p.getMoq())
                .stockQty(p.getStockQty())
                .active(p.isActive())
                .is24HourDelivery(p.is24HourDelivery())
                .rating(p.getRating())
                .reviewCount(p.getReviewCount())
                .gstRate(p.getGstRate())
                .hsnCode(p.getHsnCode())
                .specifications(p.getSpecifications())
                .bulkPricingTiers(p.getBulkPricingTiers())
                .vendor(p.getVendor())
                .createdAt(p.getCreatedAt())
                .build();

        return WishlistResponse.builder()
                .id(item.getId())
                .productId(p.getProductId())
                .product(pr)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
