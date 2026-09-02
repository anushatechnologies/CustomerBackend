package com.example.project.customer.service;

import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerPricingUpdateRequest;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductPageResponse;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.VendorInfo;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerProductServiceImpl implements SellerProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional(readOnly = true)
    public SellerProductPageResponse getSellerProducts(
            Integer sellerId,
            String search,
            Object category,
            Object brand,
            String status,
            String stockStatus,
            String sortBy,
            int page,
            int limit
    ) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 12;

        Sort sort = resolveSort(sortBy);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Seller Isolation
            Predicate matchSellerId = cb.equal(root.get("sellerId"), sellerId);
            predicates.add(matchSellerId);

            // 2. Search query
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate skuLike = cb.like(cb.lower(root.get("sku")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleLike, skuLike, descLike));
            }

            // 3. Category filter
            if (category != null) {
                String catStr = category.toString().trim();
                if (!catStr.isEmpty()) {
                    if (isNumeric(catStr)) {
                        Integer catId = Integer.parseInt(catStr);
                        predicates.add(cb.equal(root.get("brand").get("subcategory").get("category").get("categoryId"), catId));
                    } else {
                        predicates.add(cb.like(cb.lower(root.get("brand").get("subcategory").get("category").get("name")), "%" + catStr.toLowerCase() + "%"));
                    }
                }
            }

            // 4. Brand filter
            if (brand != null) {
                String brandStr = brand.toString().trim();
                if (!brandStr.isEmpty()) {
                    if (isNumeric(brandStr)) {
                        Integer bId = Integer.parseInt(brandStr);
                        predicates.add(cb.equal(root.get("brand").get("brandId"), bId));
                    } else {
                        predicates.add(cb.like(cb.lower(root.get("brand").get("name")), "%" + brandStr.toLowerCase() + "%"));
                    }
                }
            }

            // 5. Approval Status
            if (status != null && !status.trim().isEmpty()) {
                try {
                    ApprovalStatus appStatus = ApprovalStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("approvalStatus"), appStatus));
                } catch (Exception ignored) {
                }
            }

            // 6. Stock Status
            if (stockStatus != null && !stockStatus.trim().isEmpty()) {
                String normStock = stockStatus.trim().toLowerCase();
                if ("in stock".equals(normStock) || "instock".equals(normStock)) {
                    predicates.add(cb.gt(root.get("stockQty"), 10));
                } else if ("low stock".equals(normStock) || "lowstock".equals(normStock)) {
                    predicates.add(cb.and(cb.gt(root.get("stockQty"), 0), cb.le(root.get("stockQty"), 10)));
                } else if ("out of stock".equals(normStock) || "outofstock".equals(normStock)) {
                    predicates.add(cb.le(root.get("stockQty"), 0));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<ProductResponse> responses = productPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return SellerProductPageResponse.builder()
                .success(true)
                .total(productPage.getTotalElements())
                .page(page)
                .limit(pageSize)
                .data(responses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getSellerProductById(Integer sellerId, Integer productId) {
        Product product = findSellerProduct(sellerId, productId);
        return mapToResponse(product);
    }

    @Override
    public ProductResponse createSellerProduct(Integer sellerId, SellerProductCreateRequest request) {
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));

        String slug = request.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                + "-" + System.currentTimeMillis();

        BigDecimal price = request.getEffectivePrice();
        BigDecimal mrp = request.getMrp() != null ? request.getMrp() : price;

        Product product = Product.builder()
                .sellerId(sellerId)
                .brand(brand)
                .title(request.getTitle())
                .slug(slug)
                .sku(request.getSku())
                .description(request.getDescription())
                .price(price)
                .sellingPrice(price)
                .mrp(mrp)
                .unit(request.getUnit())
                .moq(request.getMoq() != null ? request.getMoq() : 1)
                .stockQty(request.getStockQty() != null ? request.getStockQty() : 0)
                .is24HourDelivery(request.getIs24HourDelivery() != null && request.getIs24HourDelivery())
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .imageUrl(request.getImages() != null && !request.getImages().isEmpty() ? request.getImages().get(0) : null)
                .bulkPricingTiers(request.getBulkPricingTiers() != null ? request.getBulkPricingTiers() : new ArrayList<>())
                .specifications(request.getSpecifications() != null ? request.getSpecifications() : new java.util.LinkedHashMap<>())
                .approvalStatus(ApprovalStatus.PENDING)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .vendor(VendorInfo.builder()
                        .vendorId(sellerId)
                        .companyName(getSellerCompanyName(sellerId))
                        .isVerified(true)
                        .rating(4.8)
                        .build())
                .build();

        Product saved = productRepository.save(product);
        log.info("Created new seller product id={} for sellerId={}", saved.getProductId(), sellerId);
        return mapToResponse(saved);
    }

    @Override
    public ProductResponse updateSellerProduct(Integer sellerId, Integer productId, SellerProductUpdateRequest request) {
        Product product = findSellerProduct(sellerId, productId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            product.setTitle(request.getTitle());
        }
        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            product.setBrand(brand);
        }
        if (request.getEffectivePrice() != null) {
            product.setPrice(request.getEffectivePrice());
            product.setSellingPrice(request.getEffectivePrice());
        }
        if (request.getMrp() != null) {
            product.setMrp(request.getMrp());
        }
        if (request.getUnit() != null) {
            product.setUnit(request.getUnit());
        }
        if (request.getMoq() != null) {
            product.setMoq(request.getMoq());
        }
        if (request.getStockQty() != null) {
            product.setStockQty(request.getStockQty());
        }
        if (request.getIs24HourDelivery() != null) {
            product.setIs24HourDelivery(request.getIs24HourDelivery());
        }
        if (request.getImages() != null) {
            product.setImages(request.getImages());
            if (!request.getImages().isEmpty()) {
                product.setImageUrl(request.getImages().get(0));
            }
        }
        if (request.getBulkPricingTiers() != null) {
            product.setBulkPricingTiers(request.getBulkPricingTiers());
        }
        if (request.getSpecifications() != null) {
            product.setSpecifications(request.getSpecifications());
        }
        product.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Override
    public ProductResponse updateSellerStock(Integer sellerId, Integer productId, Integer stockQty) {
        Product product = findSellerProduct(sellerId, productId);
        product.setStockQty(stockQty != null ? stockQty : 0);
        product.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateSellerPricing(Integer sellerId, Integer productId, SellerPricingUpdateRequest request) {
        Product product = findSellerProduct(sellerId, productId);
        if (request.getEffectiveSellingPrice() != null) {
            product.setPrice(request.getEffectiveSellingPrice());
            product.setSellingPrice(request.getEffectiveSellingPrice());
        }
        if (request.getMrp() != null) {
            product.setMrp(request.getMrp());
        }
        if (request.getBulkPricingTiers() != null) {
            product.setBulkPricingTiers(request.getBulkPricingTiers());
        }
        product.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(productRepository.save(product));
    }

    @Override
    public void deleteSellerProduct(Integer sellerId, Integer productId) {
        Product product = findSellerProduct(sellerId, productId);
        productRepository.delete(product);
        log.info("Deleted product id={} for sellerId={}", productId, sellerId);
    }

    private Product findSellerProduct(Integer sellerId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        boolean isOwner = (product.getSellerId() != null && product.getSellerId().equals(sellerId))
                || (product.getVendor() != null && product.getVendor().getVendorId() != null && product.getVendor().getVendorId().equals(sellerId));

        if (!isOwner) {
            throw new ResourceNotFoundException("Product not found or not owned by seller: " + productId);
        }
        return product;
    }

    private String getSellerCompanyName(Integer sellerId) {
        return sellerRepository.findById(sellerId)
                .map(s -> s.getCompanyName() != null ? s.getCompanyName() : s.getName())
                .orElse("Seller #" + sellerId);
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "newest".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("price-asc".equalsIgnoreCase(sortBy) || "price_asc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "price");
        }
        if ("price-desc".equalsIgnoreCase(sortBy) || "price_desc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        if ("stock-asc".equalsIgnoreCase(sortBy) || "stock_asc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "stockQty");
        }
        if ("stock-desc".equalsIgnoreCase(sortBy) || "stock_desc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "stockQty");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private ProductResponse mapToResponse(Product p) {
        Integer brandId = p.getBrand() != null ? p.getBrand().getBrandId() : null;
        String brandName = p.getBrand() != null ? p.getBrand().getName() : null;

        Integer subcategoryId = (p.getBrand() != null && p.getBrand().getSubcategory() != null)
                ? p.getBrand().getSubcategory().getSubcategoryId()
                : null;

        String subcategoryName = (p.getBrand() != null && p.getBrand().getSubcategory() != null)
                ? p.getBrand().getSubcategory().getName()
                : null;

        Integer categoryId = (p.getBrand() != null && p.getBrand().getSubcategory() != null && p.getBrand().getSubcategory().getCategory() != null)
                ? p.getBrand().getSubcategory().getCategory().getCategoryId()
                : null;

        String categoryName = (p.getBrand() != null && p.getBrand().getSubcategory() != null && p.getBrand().getSubcategory().getCategory() != null)
                ? p.getBrand().getSubcategory().getCategory().getName()
                : null;

        String status = p.getApprovalStatus() != null ? p.getApprovalStatus().name() : "PENDING";

        return ProductResponse.builder()
                .productId(p.getProductId())
                .brandId(brandId)
                .brand(brandName)
                .brandName(brandName)
                .subcategoryId(subcategoryId)
                .subcategoryName(subcategoryName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .title(p.getTitle())
                .slug(p.getSlug())
                .sku(p.getSku())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .images(p.getImages() != null ? p.getImages() : new ArrayList<>())
                .price(p.getPrice())
                .sellingPrice(p.getSellingPrice() != null ? p.getSellingPrice() : p.getPrice())
                .mrp(p.getMrp())
                .unit(p.getUnit())
                .moq(p.getMoq())
                .stockQty(p.getStockQty())
                .active(Boolean.TRUE.equals(p.getActive()))
                .is24HourDelivery(p.is24HourDelivery())
                .rating(p.getRating() != null ? p.getRating() : 0.0)
                .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                .gstRate(p.getGstRate())
                .hsnCode(p.getHsnCode())
                .specifications(p.getSpecifications() != null ? p.getSpecifications() : new java.util.LinkedHashMap<>())
                .bulkPricingTiers(p.getBulkPricingTiers() != null ? p.getBulkPricingTiers() : new ArrayList<>())
                .vendor(p.getVendor())
                .approvalStatus(status)
                .status(status)
                .sellerId("seller_" + (p.getSellerId() != null ? p.getSellerId() : 1001))
                .rejectionReason(p.getRejectionReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                .build();
    }
}
