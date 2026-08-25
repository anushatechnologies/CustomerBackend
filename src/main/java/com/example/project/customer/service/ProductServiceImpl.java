package com.example.project.customer.service;

import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import com.example.project.customer.repository.VendorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              SubcategoryRepository subcategoryRepository,
                              CategoryRepository categoryRepository,
                              VendorRepository vendorRepository) {
        this.productRepository = productRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(Integer categoryId, Integer subcategoryId, String search,
                                                      BigDecimal minPrice, BigDecimal maxPrice, String brand,
                                                      Boolean is24HourDelivery, String sort, int page, int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 20;

        Sort sortObj = determineSort(sort);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sortObj);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (subcategoryId != null) {
                predicates.add(cb.equal(root.get("subcategory").get("subcategoryId"), subcategoryId));
            }

            if (categoryId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("category").get("categoryId"), categoryId),
                        cb.equal(root.get("subcategory").get("category").get("categoryId"), categoryId)
                ));
            }

            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), term),
                        cb.like(cb.lower(root.get("brand")), term),
                        cb.like(cb.lower(root.get("description")), term),
                        cb.like(cb.lower(root.get("sku")), term)
                ));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            }

            if (Boolean.TRUE.equals(is24HourDelivery)) {
                predicates.add(cb.isTrue(root.get("is24HourDelivery")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> pagedResult = productRepository.findAll(spec, pageable);
        List<ProductResponse> dtos = pagedResult.getContent().stream().map(this::toResponse).toList();

        return PagedResponse.of(dtos, page, pageSize, pagedResult.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSuggestionsResponse getSearchSuggestions(String query) {
        String q = query != null ? query.trim() : "";
        List<Product> products = productRepository.searchByTitleOrBrand(q);

        List<SearchSuggestionsResponse.ProductSuggestion> prodSuggs = products.stream()
                .limit(5)
                .map(p -> new SearchSuggestionsResponse.ProductSuggestion(
                        p.getProductId(),
                        p.getTitle(),
                        p.getCategory() != null ? p.getCategory().getName() : (p.getSubcategory() != null && p.getSubcategory().getCategory() != null ? p.getSubcategory().getCategory().getName() : "General")
                )).toList();

        List<Category> categories = categoryRepository.findByActiveOrderBySortOrderAsc(true);
        List<SearchSuggestionsResponse.CategorySuggestion> catSuggs = categories.stream()
                .filter(c -> q.isBlank() || c.getName().toLowerCase().contains(q.toLowerCase()))
                .limit(4)
                .map(c -> new SearchSuggestionsResponse.CategorySuggestion(c.getCategoryId(), c.getName()))
                .toList();

        List<String> popularSearches = Arrays.asList(
                "Tata Tiscon 12mm",
                "UltraTech Cement",
                "Havells Armoured Cable",
                "Asian Paints Epoxy",
                "IS 1786 Fe 550D"
        );

        return new SearchSuggestionsResponse(prodSuggs, catSuggs, popularSearches);
    }

    private Sort determineSort(String sort) {
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

    private void applyRequest(Product product, ProductRequest request) {
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + request.getSubcategoryId()));
        product.setSubcategory(subcategory);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElse(subcategory.getCategory());
            product.setCategory(category);
        } else {
            product.setCategory(subcategory.getCategory());
        }

        product.setTitle(request.getTitle());
        product.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle()));
        product.setSku(request.getSku() != null ? request.getSku() : "SKU-" + System.currentTimeMillis());
        product.setBrand(request.getBrand());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        if (request.getImages() != null) {
            product.setImages(new ArrayList<>(request.getImages()));
        } else if (request.getImageUrl() != null && product.getImages().isEmpty()) {
            product.setImages(List.of(request.getImageUrl()));
        }
        product.setPrice(request.getPrice());
        product.setMrp(request.getMrp() != null ? request.getMrp() : request.getPrice().multiply(new BigDecimal("1.10")));
        product.setUnit(request.getUnit());
        product.setMoq(request.getMoq() != null ? request.getMoq() : 1);
        product.setStockQty(request.getStockQty() != null ? request.getStockQty() : 100);
        product.setActive(request.getActive());
        product.setIs24HourDelivery(request.getIs24HourDelivery());
        product.setRating(request.getRating() != null ? request.getRating() : 4.8);
        product.setReviewCount(request.getReviewCount() != null ? request.getReviewCount() : 10);
        product.setGstRate(request.getGstRate() != null ? request.getGstRate() : 18.0);
        product.setHsnCode(request.getHsnCode());

        if (request.getSpecifications() != null) {
            product.setSpecifications(request.getSpecifications());
        }

        if (request.getVendorId() != null) {
            Vendor vendor = vendorRepository.findById(request.getVendorId()).orElse(null);
            product.setVendor(vendor);
        }

        if (request.getBulkPricingTiers() != null) {
            product.getBulkPricingTiers().clear();
            for (BulkPricingTierDto tierDto : request.getBulkPricingTiers()) {
                BulkPricingTier tier = new BulkPricingTier(
                        tierDto.getMinQty(),
                        tierDto.getMaxQty(),
                        tierDto.getPrice(),
                        tierDto.getDiscountPercentage()
                );
                product.addBulkPricingTier(tier);
            }
        }
    }

    private ProductResponse toResponse(Product p) {
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
        resp.setSpecifications(p.getSpecifications());

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

    private String generateSlug(String text) {
        if (text == null) return "prod-" + System.currentTimeMillis();
        return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") + "-" + System.currentTimeMillis() % 10000;
    }
}