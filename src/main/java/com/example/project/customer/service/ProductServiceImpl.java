package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SearchSuggestionResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public ProductResponse create(ProductRequest request) {
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + request.getSubcategoryId()));

        Product product = new Product();
        mapRequestToProduct(product, request, subcategory);
        return mapToResponse(repository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        Product product = findProduct(id);
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProductResponse>> getAll(
            Integer categoryId,
            Integer subcategoryId,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String brand,
            Boolean is24HourDelivery,
            String sort,
            int page,
            int limit
    ) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 20;

        Sort sorting = getSort(sort);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sorting);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Active filter
            predicates.add(cb.isTrue(root.get("active")));

            // Filter by Category ID
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("subcategory").get("category").get("categoryId"), categoryId));
            }

            // Filter by Subcategory ID (Fixing Bug 2)
            if (subcategoryId != null) {
                predicates.add(cb.equal(root.get("subcategory").get("subcategoryId"), subcategoryId));
            }

            // Search by text in title, brand, or description
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), pattern);
                Predicate brandPred = cb.like(cb.lower(root.get("brand")), pattern);
                Predicate descPred = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titlePred, brandPred, descPred));
            }

            // Price range filter
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Brand filter
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            }

            // 24 Hour Express Delivery filter
            if (Boolean.TRUE.equals(is24HourDelivery)) {
                predicates.add(cb.isTrue(root.get("is24HourDelivery")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = repository.findAll(spec, pageable);
        List<ProductResponse> data = productPage.getContent().stream().map(this::mapToResponse).toList();
        PaginationMeta pagination = PaginationMeta.of(page > 0 ? page : 1, pageSize, productPage.getTotalElements());

        return ApiResponse.paginated(data, pagination);
    }

    @Override
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = findProduct(id);
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + request.getSubcategoryId()));

        mapRequestToProduct(product, request, subcategory);
        return mapToResponse(repository.save(product));
    }

    @Override
    public void delete(Integer id) {
        Product product = findProduct(id);
        repository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSuggestionResponse getSearchSuggestions(String query) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isEmpty()) {
            return SearchSuggestionResponse.builder()
                    .products(List.of())
                    .categories(List.of())
                    .popularSearches(List.of("UltraTech Cement", "Tata Tiscon 12mm", "Armoured Cable", "CPVC Pipes", "JSW Sheets"))
                    .build();
        }

        List<Product> products = repository.findTop5ByTitleContainingIgnoreCaseOrBrandContainingIgnoreCase(trimmed, trimmed);
        List<SearchSuggestionResponse.ProductSuggestion> productSuggestions = products.stream()
                .map(p -> SearchSuggestionResponse.ProductSuggestion.builder()
                        .productId(p.getProductId())
                        .title(p.getTitle())
                        .category(p.getSubcategory() != null && p.getSubcategory().getCategory() != null
                                ? p.getSubcategory().getCategory().getName() : "")
                        .build())
                .toList();

        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(c -> c.getName().toLowerCase().contains(trimmed.toLowerCase()))
                .limit(5)
                .toList();

        List<SearchSuggestionResponse.CategorySuggestion> categorySuggestions = categories.stream()
                .map(c -> SearchSuggestionResponse.CategorySuggestion.builder()
                        .categoryId(c.getCategoryId())
                        .name(c.getName())
                        .build())
                .toList();

        return SearchSuggestionResponse.builder()
                .products(productSuggestions)
                .categories(categorySuggestions)
                .popularSearches(List.of("UltraTech Cement", "Tata Tiscon 12mm", "Armoured Cable", "CPVC Pipes"))
                .build();
    }

    private Product findProduct(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Sort getSort(String sort) {
        if ("price_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "price");
        } else if ("rating".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        } else {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    private void mapRequestToProduct(Product product, ProductRequest req, Subcategory subcategory) {
        product.setSubcategory(subcategory);
        product.setTitle(req.getTitle());
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            product.setSlug(req.getSlug().trim());
        } else if (product.getSlug() == null) {
            product.setSlug(req.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
        }
        product.setSku(req.getSku());
        product.setBrand(req.getBrand());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setMrp(req.getMrp() != null ? req.getMrp() : req.getPrice());
        product.setStockQty(req.getStockQty());
        product.setUnit(req.getUnit());
        product.setMoq(req.getMoq() != null ? req.getMoq() : 1);
        product.setImageUrl(req.getImageUrl());
        product.setImages(req.getImages() != null ? req.getImages() : new ArrayList<>());
        product.setActive(req.getActive() != null ? req.getActive() : true);
        product.setIs24HourDelivery(req.getIs24HourDelivery() != null ? req.getIs24HourDelivery() : false);
        product.setRating(req.getRating() != null ? req.getRating() : 4.5);
        product.setReviewCount(req.getReviewCount() != null ? req.getReviewCount() : 0);
        product.setGstRate(req.getGstRate() != null ? req.getGstRate() : BigDecimal.valueOf(18.0));
        product.setHsnCode(req.getHsnCode());
        product.setSpecifications(req.getSpecifications() != null ? req.getSpecifications() : new java.util.LinkedHashMap<>());
        product.setBulkPricingTiers(req.getBulkPricingTiers() != null ? req.getBulkPricingTiers() : new ArrayList<>());
        product.setVendor(req.getVendor());
    }

    private ProductResponse mapToResponse(Product p) {
        Integer catId = (p.getSubcategory() != null && p.getSubcategory().getCategory() != null)
                ? p.getSubcategory().getCategory().getCategoryId() : null;

        return ProductResponse.builder()
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
    }
}