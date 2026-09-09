package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.StoreResponse;
import com.example.project.customer.dto.StoreStatusUpdateRequest;
import com.example.project.customer.dto.StoreUpdateRequest;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public ApiResponse<List<StoreResponse>> getActiveStores(int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "rating"));

        Page<Store> pageResult = storeRepository.findByStatus(StoreStatus.ACTIVE, pageable);
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
    public ApiResponse<List<ProductResponse>> getStoreProducts(String slugOrId, String search, Integer categoryId, int page, int limit, String sortBy) {
        Store store;
        try {
            int storeId = Integer.parseInt(slugOrId);
            store = storeRepository.findById(storeId)
                    .orElseGet(() -> storeRepository.findBySlugIgnoreCase(slugOrId)
                            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + slugOrId)));
        } catch (NumberFormatException e) {
            store = storeRepository.findBySlugIgnoreCase(slugOrId)
                    .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + slugOrId));
        }

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
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("store").get("storeId"), targetStoreId));
            predicates.add(cb.equal(root.get("active"), true));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("brand").get("subcategory").get("category").get("categoryId"), categoryId));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), term),
                        cb.like(cb.lower(root.get("description")), term),
                        cb.like(cb.lower(root.get("sku")), term)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Product> pageResult = productRepository.findAll(spec, pageable);
        List<ProductResponse> products = pageResult.getContent().stream()
                .map(productService::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Store products retrieved successfully for " + store.getName(), products, meta);
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
