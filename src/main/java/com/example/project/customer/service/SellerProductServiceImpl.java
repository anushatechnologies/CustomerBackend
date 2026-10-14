package com.example.project.customer.service;

import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerPricingUpdateRequest;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductPageResponse;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.entity.Store;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.Map;

@Slf4j
@Service
@Transactional
public class SellerProductServiceImpl implements SellerProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final SellerRepository sellerRepository;
    private final StoreRepository storeRepository;
    private final ProductTaxonomyService productTaxonomyService;

    public SellerProductServiceImpl(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            SellerRepository sellerRepository,
            StoreRepository storeRepository) {
        this(productRepository, brandRepository, sellerRepository, storeRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SellerProductServiceImpl(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            SellerRepository sellerRepository,
            StoreRepository storeRepository,
            ProductTaxonomyService productTaxonomyService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.sellerRepository = sellerRepository;
        this.storeRepository = storeRepository;
        this.productTaxonomyService = productTaxonomyService;
    }

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
            int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 12;

        Sort sort = resolveSort(sortBy);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Seller Isolation
            Predicate matchSellerId = cb.equal(root.get("seller").get("sellerId"), sellerId);
            predicates.add(matchSellerId);

            // 2. Search query
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate skuLike = cb.like(cb.lower(root.get("sku")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleLike, skuLike, descLike));
            }

            // 3. Category filter via direct subcategory or brand -> subcategory -> category
            if (category != null) {
                String catStr = category.toString().trim();
                if (!catStr.isEmpty()) {
                    jakarta.persistence.criteria.Join<Product, Subcategory> subJoin = root.join("subcategory", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Join<Subcategory, Category> subCatJoin = subJoin.join("category", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Join<Product, Brand> brandJoin = root.join("brand", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Join<Brand, Subcategory> brandSubJoin = brandJoin.join("subcategory", jakarta.persistence.criteria.JoinType.LEFT);
                    jakarta.persistence.criteria.Join<Subcategory, Category> brandCatJoin = brandSubJoin.join("category", jakarta.persistence.criteria.JoinType.LEFT);

                    if (isNumeric(catStr)) {
                        Integer catId = Integer.parseInt(catStr);
                        Predicate directCat = cb.equal(subCatJoin.get("categoryId"), catId);
                        Predicate brandCat = cb.and(
                                cb.isNull(root.get("subcategory")),
                                cb.equal(brandCatJoin.get("categoryId"), catId)
                        );
                        predicates.add(cb.or(directCat, brandCat));
                    } else {
                        Predicate directCatLike = cb.like(cb.lower(subCatJoin.get("name")),
                                "%" + catStr.toLowerCase() + "%");
                        Predicate brandCatLike = cb.and(
                                cb.isNull(root.get("subcategory")),
                                cb.like(cb.lower(brandCatJoin.get("name")),
                                        "%" + catStr.toLowerCase() + "%")
                        );
                        predicates.add(cb.or(directCatLike, brandCatLike));
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
                        predicates.add(
                                cb.like(cb.lower(root.get("brand").get("name")), "%" + brandStr.toLowerCase() + "%"));
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

        Map<String, String> normalizedSpecs = cleanSpecifications(request.getSpecifications());

        String slug = request.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                + "-" + System.currentTimeMillis();

        BigDecimal price = request.getEffectivePrice();
        BigDecimal mrp = request.getMrp();
        if (mrp != null && price != null && mrp.compareTo(price) < 0) {
            throw new IllegalArgumentException("Selling price cannot exceed MRP (Maximum Retail Price).");
        }

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        Store store = storeRepository.findBySellerSellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("No active store found for seller id: " + sellerId));

        ApprovalStatus initialStatus = com.example.project.customer.security.SecurityUtils.isAdmin()
                ? ApprovalStatus.APPROVED
                : ApprovalStatus.PENDING;

        Subcategory subcategory = null;
        if (productTaxonomyService != null) {
            subcategory = productTaxonomyService.resolveSubcategoryForProduct(
                    request.getSubcategoryId(),
                    request.getCategoryId(),
                    null,
                    brand
            );
        }

        Product product = Product.builder()
                .seller(seller)
                .store(store)
                .brand(brand)
                .subcategory(subcategory)
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
                .imageUrl(request.getImages() != null && !request.getImages().isEmpty() ? request.getImages().get(0)
                        : null)
                .bulkPricingTiers(
                        request.getBulkPricingTiers() != null ? request.getBulkPricingTiers() : new ArrayList<>())
                .specifications(normalizedSpecs)
                .approvalStatus(initialStatus)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product saved = productRepository.save(product);
        log.info("Created new seller product id={} for sellerId={} in storeId={} (status={})", saved.getProductId(),
                sellerId, store != null ? store.getStoreId() : null, initialStatus);
        return mapToResponse(saved);
    }

    @Override
    public ProductResponse updateSellerProduct(Integer sellerId, Integer productId,
            SellerProductUpdateRequest request) {
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
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Brand not found with id: " + request.getBrandId()));
            product.setBrand(brand);
        }
        if (request.getSubcategoryId() != null || request.getCategoryId() != null) {
            if (productTaxonomyService != null) {
                Subcategory subcategory = productTaxonomyService.resolveSubcategoryForProduct(
                        request.getSubcategoryId(),
                        request.getCategoryId(),
                        product,
                        product.getBrand()
                );
                product.setSubcategory(subcategory);
            }
        }
        if (request.getEffectivePrice() != null) {
            product.setPrice(request.getEffectivePrice());
            product.setSellingPrice(request.getEffectivePrice());
        }
        if (request.getMrp() != null) {
            BigDecimal effectivePrice = product.getPrice();
            if (effectivePrice != null && request.getMrp().compareTo(effectivePrice) < 0) {
                throw new IllegalArgumentException("Selling price cannot exceed MRP (Maximum Retail Price).");
            }
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
            product.setSpecifications(cleanSpecifications(request.getSpecifications()));
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
    public ProductResponse updateSellerPricing(Integer sellerId, Integer productId,
            SellerPricingUpdateRequest request) {
        Product product = findSellerProduct(sellerId, productId);
        if (request.getEffectiveSellingPrice() != null) {
            product.setPrice(request.getEffectiveSellingPrice());
            product.setSellingPrice(request.getEffectiveSellingPrice());
        }
        if (request.getMrp() != null) {
            BigDecimal effectivePrice = request.getEffectiveSellingPrice() != null ? request.getEffectiveSellingPrice() : product.getPrice();
            if (effectivePrice != null && request.getMrp().compareTo(effectivePrice) < 0) {
                throw new IllegalArgumentException("Selling price cannot exceed MRP (Maximum Retail Price).");
            }
            product.setMrp(request.getMrp());
        }
        if (request.getBulkPricingTiers() != null) {
            product.setBulkPricingTiers(request.getBulkPricingTiers());
        }
        product.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "brands", allEntries = true)
    public void deleteSellerProduct(Integer sellerId, Integer productId) {
        Product product = findSellerProduct(sellerId, productId);
        product.setActive(false);
        product.setApprovalStatus(ApprovalStatus.REJECTED);
        product.setRejectionReason("Removed from inventory by seller");
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Soft-deleted (deactivated) product id={} for sellerId={}", productId, sellerId);
    }

    private Product findSellerProduct(Integer sellerId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        boolean isOwner = product.getSeller() != null
                && product.getSeller().getSellerId() != null
                && product.getSeller().getSellerId().equals(sellerId);

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
        if (str == null)
            return false;
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

        com.example.project.customer.entity.Subcategory subcategory = p.getSubcategory();
        Integer subcategoryId = subcategory != null ? subcategory.getSubcategoryId() : null;
        String subcategoryName = subcategory != null ? subcategory.getName() : null;

        com.example.project.customer.entity.Category category = p.getCategory();
        Integer categoryId = category != null ? category.getCategoryId() : null;
        String categoryName = category != null ? category.getName() : null;

        String status = p.getApprovalStatus() != null ? p.getApprovalStatus().name() : "PENDING";

        Integer storeId = p.getStore() != null ? p.getStore().getStoreId() : null;
        String storeName = p.getStore() != null ? p.getStore().getStoreName() : null;
        String storeSlug = p.getStore() != null ? p.getStore().getSlug() : null;

        return ProductResponse.builder()
                .productId(p.getProductId())
                .storeId(storeId)
                .storeName(storeName)
                .storeSlug(storeSlug)
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
                .sellerId(p.getSeller() != null && p.getSeller().getSellerId() != null
                        ? "seller_" + p.getSeller().getSellerId()
                        : "seller_1001")
                .sellerName(p.getSeller() != null ? p.getSeller().getName() : null)
                .approvalStatus(status)
                .status(status)
                .rejectionReason(p.getRejectionReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                .build();
    }

    private Map<String, String> cleanSpecifications(Map<String, String> specs) {
        if (specs == null) {
            return new java.util.LinkedHashMap<>();
        }
        Map<String, String> cleaned = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().trim().isBlank()
                    && entry.getValue() != null && !entry.getValue().trim().isBlank()) {
                cleaned.put(entry.getKey().trim(), entry.getValue().trim());
            }
        }
        return cleaned;
    }
}
