package com.example.project.customer.service;

import com.example.project.customer.dto.BulkPricingTierDto;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.VendorDto;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.WishlistItem;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlist(Integer userId) {
        Integer uid = userId != null ? userId : 101;
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(uid);
        return items.stream().map(i -> toProductResponse(i.getProduct())).toList();
    }

    @Override
    public void addToWishlist(Integer userId, Integer productId) {
        Integer uid = userId != null ? userId : 101;
        if (!wishlistRepository.existsByUserIdAndProduct_ProductId(uid, productId)) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            WishlistItem item = new WishlistItem(uid, product);
            wishlistRepository.save(item);
        }
    }

    @Override
    public void removeFromWishlist(Integer userId, Integer productId) {
        Integer uid = userId != null ? userId : 101;
        wishlistRepository.findByUserIdAndProduct_ProductId(uid, productId)
                .ifPresent(wishlistRepository::delete);
    }

    private ProductResponse toProductResponse(Product p) {
        ProductResponse resp = new ProductResponse();
        resp.setProductId(p.getProductId());
        resp.setSubcategoryId(p.getSubcategory() != null ? p.getSubcategory().getSubcategoryId() : null);
        resp.setCategoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : (p.getSubcategory() != null && p.getSubcategory().getCategory() != null ? p.getSubcategory().getCategory().getCategoryId() : null));
        resp.setTitle(p.getTitle());
        resp.setSlug(p.getSlug());
        resp.setSku(p.getSku());
        resp.setBrand(p.getBrand());
        resp.setDescription(p.getDescription());
        resp.setImageUrl(p.getImageUrl());
        resp.setImages(p.getImages());
        resp.setPrice(p.getPrice());
        resp.setMrp(p.getMrp());
        resp.setUnit(p.getUnit());
        resp.setMoq(p.getMoq());
        resp.setStockQty(p.getStockQty());
        resp.setActive(p.isActive());
        resp.setIs24HourDelivery(p.isIs24HourDelivery());
        resp.setRating(p.getRating());
        resp.setReviewCount(p.getReviewCount());
        resp.setGstRate(p.getGstRate());
        resp.setHsnCode(p.getHsnCode());

        if (p.getBulkPricingTiers() != null) {
            resp.setBulkPricingTiers(p.getBulkPricingTiers().stream().map(t ->
                    new BulkPricingTierDto(t.getTierId(), t.getMinQty(), t.getMaxQty(), t.getPrice(), t.getDiscountPercentage())
            ).toList());
        }

        if (p.getVendor() != null) {
            resp.setVendor(new VendorDto(
                    p.getVendor().getVendorId(),
                    p.getVendor().getCompanyName(),
                    p.getVendor().getCity(),
                    p.getVendor().isVerified(),
                    p.getVendor().getRating()
            ));
        }

        resp.setCreatedAt(p.getCreatedAt());
        return resp;
    }
}
