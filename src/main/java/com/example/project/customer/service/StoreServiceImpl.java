package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.StoreResponse;
import com.example.project.customer.dto.StoreStatusUpdateRequest;
import com.example.project.customer.dto.StoreUpdateRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<StoreResponse>> getActiveStores(String search, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "rating"));

        Specification<Store> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), StoreStatus.ACTIVE));

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("slug")), term),
                        cb.like(cb.lower(root.get("description")), term)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Store> pageResult = storeRepository.findAll(spec, pageable);
        List<StoreResponse> responses = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Active marketplace stores retrieved successfully", responses, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreBySlug(String slug) {
        Store store = storeRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with slug: " + slug));
        return mapToResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Integer storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));
        return mapToResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getSellerStore(Integer sellerId) {
        Store store = storeRepository.findBySeller_SellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("No store profile found for seller ID: " + sellerId));
        return mapToResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse updateSellerStore(Integer sellerId, StoreUpdateRequest request) {
        Store store = storeRepository.findBySeller_SellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("No store found for seller ID: " + sellerId));

        store.setName(request.getName().trim());
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl().trim());
        }
        if (request.getBannerUrl() != null) {
            store.setBannerUrl(request.getBannerUrl().trim());
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription().trim());
        }
        if (request.getMinOrderValue() != null) {
            store.setMinOrderValue(request.getMinOrderValue());
        }
        if (request.getServiceRadiusKm() != null) {
            store.setServiceRadiusKm(request.getServiceRadiusKm());
        }

        Store saved = storeRepository.save(store);
        log.info("Updated store #{} ('{}') for seller #{}", saved.getStoreId(), saved.getName(), sellerId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public StoreResponse updateStoreStatusByAdmin(Integer storeId, StoreStatusUpdateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + storeId));

        store.setStatus(request.getStatus());
        Store saved = storeRepository.save(store);
        log.info("Admin updated Store #{} status to {} (Remarks: {})", storeId, request.getStatus(), request.getRemarks());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CategoryResponse>> getStoreCategories(String slugOrId) {
        Store store = resolveStore(slugOrId);
        Integer storeId = store.getStoreId();

        List<Product> products = productRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("store").get("storeId"), storeId),
                cb.equal(root.get("active"), true)
        ));

        Map<Integer, Category> categoryMap = new LinkedHashMap<>();
        Map<Integer, Integer> categoryProductCounts = new HashMap<>();
        Map<Integer, Map<Integer, Subcategory>> subcategoryMap = new HashMap<>();
        Map<Integer, Integer> subcategoryProductCounts = new HashMap<>();

        for (Product product : products) {
            if (product.getBrand() != null && product.getBrand().getSubcategory() != null) {
                Subcategory subcategory = product.getBrand().getSubcategory();
                Category category = subcategory.getCategory();

                if (category != null) {
                    categoryMap.putIfAbsent(category.getCategoryId(), category);
                    categoryProductCounts.merge(category.getCategoryId(), 1, Integer::sum);

                    subcategoryMap.computeIfAbsent(category.getCategoryId(), k -> new LinkedHashMap<>())
                            .putIfAbsent(subcategory.getSubcategoryId(), subcategory);
                    subcategoryProductCounts.merge(subcategory.getSubcategoryId(), 1, Integer::sum);
                }
            }
        }

        List<CategoryResponse> responses = categoryMap.values().stream()
                .map(cat -> {
                    Map<Integer, Subcategory> subs = subcategoryMap.getOrDefault(cat.getCategoryId(), Collections.emptyMap());
                    List<SubcategoryResponse> subResponses = subs.values().stream()
                            .map(sub -> SubcategoryResponse.builder()
                                    .subcategoryId(sub.getSubcategoryId())
                                    .categoryId(cat.getCategoryId())
                                    .name(sub.getName())
                                    .slug(sub.getSlug())
                                    .imageUrl(sub.getImageUrl())
                                    .active(sub.isActive())
                                    .sortOrder(sub.getSortOrder())
                                    .productCount(subcategoryProductCounts.getOrDefault(sub.getSubcategoryId(), 0))
                                    .createdAt(sub.getCreatedAt())
                                    .build())
                            .sorted(Comparator.comparing(SubcategoryResponse::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(SubcategoryResponse::getName))
                            .toList();

                    return CategoryResponse.builder()
                            .categoryId(cat.getCategoryId())
                            .name(cat.getName())
                            .slug(cat.getSlug())
                            .imageUrl(cat.getImageUrl())
                            .active(cat.isActive())
                            .sortOrder(cat.getSortOrder())
                            .productCount(categoryProductCounts.getOrDefault(cat.getCategoryId(), 0))
                            .subcategories(subResponses)
                            .createdAt(cat.getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparing(CategoryResponse::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(CategoryResponse::getName))
                .toList();

        return ApiResponse.ok("Store categories retrieved successfully for " + store.getName(), responses);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProductResponse>> getStoreProducts(String slugOrId, String search, Integer categoryId, Integer subcategoryId, int page, int limit, String sortBy) {
        Store store = resolveStore(slugOrId);
        final Integer targetStoreId = store.getStoreId();
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;

        Sort sort = switch (sortBy != null ? sortBy.toLowerCase() : "featured") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating" -> Sort.by(Sort.Direction.DESC, "rating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "productId"));
        };

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("store").get("storeId"), targetStoreId));
            predicates.add(cb.equal(root.get("active"), true));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("brand").get("subcategory").get("category").get("categoryId"), categoryId));
            }

            if (subcategoryId != null) {
                predicates.add(cb.equal(root.get("brand").get("subcategory").get("subcategoryId"), subcategoryId));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), term),
                        cb.like(cb.lower(root.get("description")), term),
                        cb.like(cb.lower(root.get("sku")), term)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> pageResult = productRepository.findAll(spec, pageable);
        List<ProductResponse> products = pageResult.getContent().stream()
                .map(productService::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Store products retrieved successfully for " + store.getName(), products, meta);
    }

    private Store resolveStore(String slugOrId) {
        try {
            int storeId = Integer.parseInt(slugOrId);
            return storeRepository.findById(storeId)
                    .orElseGet(() -> storeRepository.findBySlugIgnoreCase(slugOrId)
                            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + slugOrId)));
        } catch (NumberFormatException e) {
            return storeRepository.findBySlugIgnoreCase(slugOrId)
                    .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + slugOrId));
        }
    }

    private StoreResponse mapToResponse(Store s) {
        Seller seller = s.getSeller();
        return StoreResponse.builder()
                .storeId(s.getStoreId())
                .sellerId(seller != null ? seller.getSellerId() : null)
                .sellerName(seller != null ? seller.getName() : null)
                .sellerCompanyName(seller != null ? seller.getCompanyName() : null)
                .name(s.getName())
                .slug(s.getSlug())
                .logoUrl(s.getLogoUrl())
                .bannerUrl(s.getBannerUrl())
                .description(s.getDescription())
                .status(s.getStatus())
                .minOrderValue(s.getMinOrderValue())
                .serviceRadiusKm(s.getServiceRadiusKm())
                .commissionRate(s.getCommissionRate())
                .rating(s.getRating())
                .reviewCount(s.getReviewCount())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
