package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SearchSuggestionResponse;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
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
    private final S3ImageService s3ImageService;

    @Override
    public ProductResponse create(ProductRequest request) {

        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subcategory not found with id: " + request.getSubcategoryId()
                        ));

        Product product = new Product();

        mapRequestToProduct(product, request, subcategory);

        // IMPORTANT:
        // Every newly submitted product must wait for admin approval.
        product.setApprovalStatus(ApprovalStatus.PENDING);
        product.setActive(false);
        product.setRejectionReason(null);

        return mapToResponse(repository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {

        Product product = repository
                .findByProductIdAndApprovalStatusAndActive(
                        id,
                        ApprovalStatus.APPROVED,
                        true
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        ));

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

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                sorting
        );

        Specification<Product> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Only approved + active products are visible to customers
            predicates.add(
                    cb.equal(
                            root.get("approvalStatus"),
                            ApprovalStatus.APPROVED
                    )
            );

            predicates.add(
                    cb.isTrue(root.get("active"))
            );

            // Category filter
            if (categoryId != null) {
                predicates.add(
                        cb.equal(
                                root.get("subcategory")
                                        .get("category")
                                        .get("categoryId"),
                                categoryId
                        )
                );
            }

            // Subcategory filter
            if (subcategoryId != null) {
                predicates.add(
                        cb.equal(
                                root.get("subcategory")
                                        .get("subcategoryId"),
                                subcategoryId
                        )
                );
            }

            // Search
            if (search != null && !search.isBlank()) {

                String pattern =
                        "%" + search.trim().toLowerCase() + "%";

                Predicate titlePredicate =
                        cb.like(
                                cb.lower(root.get("title")),
                                pattern
                        );

                Predicate brandPredicate =
                        cb.like(
                                cb.lower(root.get("brand")),
                                pattern
                        );

                Predicate descriptionPredicate =
                        cb.like(
                                cb.lower(root.get("description")),
                                pattern
                        );

                predicates.add(
                        cb.or(
                                titlePredicate,
                                brandPredicate,
                                descriptionPredicate
                        )
                );
            }

            // Minimum price
            if (minPrice != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("price"),
                                minPrice
                        )
                );
            }

            // Maximum price
            if (maxPrice != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("price"),
                                maxPrice
                        )
                );
            }

            // Brand
            if (brand != null && !brand.isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("brand")),
                                brand.trim().toLowerCase()
                        )
                );
            }

            // 24-hour delivery
            if (Boolean.TRUE.equals(is24HourDelivery)) {
                predicates.add(
                        cb.isTrue(
                                root.get("is24HourDelivery")
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };

        Page<Product> productPage =
                repository.findAll(spec, pageable);

        List<ProductResponse> data =
                productPage.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        PaginationMeta pagination =
                PaginationMeta.of(
                        page > 0 ? page : 1,
                        pageSize,
                        productPage.getTotalElements()
                );

        return ApiResponse.paginated(
                data,
                pagination
        );
    }

    @Override
    public ProductResponse update(
            Integer id,
            ProductRequest request
    ) {

        Product product = findProduct(id);
        String oldMainImage = product.getImageUrl();
        List<String> oldGalleryImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : List.of();

        Subcategory subcategory =
                subcategoryRepository.findById(
                        request.getSubcategoryId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subcategory not found with id: "
                                        + request.getSubcategoryId()
                        ));

        mapRequestToProduct(
                product,
                request,
                subcategory
        );

        /*
         * Do NOT allow a normal product update to approve
         * or activate a pending/rejected product.
         */
        if (product.getApprovalStatus() != ApprovalStatus.APPROVED) {
            product.setActive(false);
        }

        Product saved = repository.save(product);

        // Clean up old main image if replaced and no longer in use
        String newMainImage = saved.getImageUrl();
        List<String> newGalleryImages = saved.getImages() != null ? saved.getImages() : List.of();

        if (oldMainImage != null && !oldMainImage.isBlank() && !oldMainImage.equals(newMainImage) && !newGalleryImages.contains(oldMainImage)) {
            s3ImageService.deleteImage(oldMainImage);
        }

        // Clean up old gallery images that are removed and not used as main image
        for (String oldImg : oldGalleryImages) {
            if (oldImg != null && !oldImg.isBlank() && !newGalleryImages.contains(oldImg) && !oldImg.equals(newMainImage)) {
                s3ImageService.deleteImage(oldImg);
            }
        }

        return mapToResponse(saved);
    }

    @Override
    public void delete(Integer id) {

        Product product = findProduct(id);
        String mainImage = product.getImageUrl();
        List<String> galleryImages = product.getImages() != null ? new ArrayList<>(product.getImages()) : List.of();

        repository.delete(product);

        if (mainImage != null && !mainImage.isBlank()) {
            s3ImageService.deleteImage(mainImage);
        }
        for (String galleryImg : galleryImages) {
            if (galleryImg != null && !galleryImg.isBlank() && !galleryImg.equals(mainImage)) {
                s3ImageService.deleteImage(galleryImg);
            }
        }
    }

    // =========================================================
    // SEARCH SUGGESTIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public SearchSuggestionResponse getSearchSuggestions(
            String query
    ) {

        String trimmed =
                query != null ? query.trim() : "";

        if (trimmed.isEmpty()) {

            return SearchSuggestionResponse.builder()
                    .products(List.of())
                    .categories(List.of())
                    .popularSearches(
                            List.of(
                                    "UltraTech Cement",
                                    "Tata Tiscon 12mm",
                                    "Armoured Cable",
                                    "CPVC Pipes",
                                    "JSW Sheets"
                            )
                    )
                    .build();
        }

        List<Product> products =
                repository
                        .findTop5ByTitleContainingIgnoreCaseOrBrandContainingIgnoreCase(
                                trimmed,
                                trimmed
                        );

        List<SearchSuggestionResponse.ProductSuggestion>
                productSuggestions =
                products.stream()
                        .filter(product ->
                                product.getApprovalStatus()
                                        == ApprovalStatus.APPROVED
                                        && Boolean.TRUE.equals(
                                        product.getActive()
                                )
                        )
                        .map(product ->
                                SearchSuggestionResponse.ProductSuggestion
                                        .builder()
                                        .productId(product.getProductId())
                                        .title(product.getTitle())
                                        .category(
                                                product.getSubcategory() != null
                                                        && product.getSubcategory()
                                                        .getCategory() != null
                                                        ? product.getSubcategory()
                                                        .getCategory()
                                                        .getName()
                                                        : ""
                                        )
                                        .build()
                        )
                        .toList();

        List<Category> categories =
                categoryRepository
                        .findByActiveTrueOrderBySortOrderAsc()
                        .stream()
                        .filter(category ->
                                category.getName()
                                        .toLowerCase()
                                        .contains(
                                                trimmed.toLowerCase()
                                        )
                        )
                        .limit(5)
                        .toList();

        List<SearchSuggestionResponse.CategorySuggestion>
                categorySuggestions =
                categories.stream()
                        .map(category ->
                                SearchSuggestionResponse
                                        .CategorySuggestion
                                        .builder()
                                        .categoryId(
                                                category.getCategoryId()
                                        )
                                        .name(category.getName())
                                        .build()
                        )
                        .toList();

        return SearchSuggestionResponse.builder()
                .products(productSuggestions)
                .categories(categorySuggestions)
                .popularSearches(
                        List.of(
                                "UltraTech Cement",
                                "Tata Tiscon 12mm",
                                "Armoured Cable",
                                "CPVC Pipes"
                        )
                )
                .build();
    }

    // =========================================================
    // ADMIN PRODUCT MANAGEMENT
    // =========================================================

    @Override
    public ProductResponse activate(Integer id) {

        Product product = findProduct(id);

        if (product.getApprovalStatus()
                != ApprovalStatus.APPROVED) {

            throw new ResourceConflictException(
                    "Product cannot be activated because it is not approved."
            );
        }

        product.setActive(true);

        return mapToResponse(
                repository.save(product)
        );
    }

    @Override
    public ProductResponse deactivate(Integer id) {

        Product product = findProduct(id);

        product.setActive(false);

        return mapToResponse(
                repository.save(product)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductListResponse getPending() {

        List<ProductResponse> products =
                repository
                        .findByApprovalStatus(
                                ApprovalStatus.PENDING
                        )
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return new ProductListResponse(
                products,
                products.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductListResponse getAdminAll() {

        List<ProductResponse> products =
                repository
                        .findAll()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return new ProductListResponse(
                products,
                products.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getAdminById(Integer id) {

        return mapToResponse(
                findProduct(id)
        );
    }

    @Override
    public ProductResponse approve(Integer id) {

        Product product = findProduct(id);

        product.setApprovalStatus(
                ApprovalStatus.APPROVED
        );

        product.setRejectionReason(null);

        // Approval makes the product visible to customers.
        product.setActive(true);

        return mapToResponse(
                repository.save(product)
        );
    }

    @Override
    public ProductResponse reject(
            Integer id,
            ProductRejectionRequest request
    ) {

        Product product = findProduct(id);

        product.setApprovalStatus(
                ApprovalStatus.REJECTED
        );

        product.setActive(false);

        product.setRejectionReason(
                request.reason()
        );

        return mapToResponse(
                repository.save(product)
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Product findProduct(Integer id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        ));
    }

    private Sort getSort(String sort) {

        if ("price_asc".equalsIgnoreCase(sort)) {

            return Sort.by(
                    Sort.Direction.ASC,
                    "price"
            );

        } else if ("price_desc".equalsIgnoreCase(sort)) {

            return Sort.by(
                    Sort.Direction.DESC,
                    "price"
            );

        } else if ("rating".equalsIgnoreCase(sort)) {

            return Sort.by(
                    Sort.Direction.DESC,
                    "rating"
            );

        } else {

            return Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );
        }
    }

    private void mapRequestToProduct(
            Product product,
            ProductRequest req,
            Subcategory subcategory
    ) {

        product.setSubcategory(subcategory);

        product.setTitle(req.getTitle());

        if (req.getSlug() != null
                && !req.getSlug().isBlank()) {

            product.setSlug(
                    req.getSlug().trim()
            );

        } else if (product.getSlug() == null) {

            product.setSlug(
                    req.getTitle()
                            .toLowerCase()
                            .replaceAll(
                                    "[^a-z0-9]+",
                                    "-"
                            )
                            .replaceAll(
                                    "^-|-$",
                                    ""
                            )
            );
        }

        product.setSku(req.getSku());

        product.setBrand(req.getBrand());

        product.setDescription(
                req.getDescription()
        );

        product.setPrice(
                req.getPrice()
        );

        product.setMrp(
                req.getMrp() != null
                        ? req.getMrp()
                        : req.getPrice()
        );

        product.setStockQty(
                req.getStockQty()
        );

        product.setUnit(
                req.getUnit()
        );

        product.setMoq(
                req.getMoq() != null
                        ? req.getMoq()
                        : 1
        );

        product.setImageUrl(
                req.getImageUrl()
        );

        product.setImages(
                req.getImages() != null
                        ? req.getImages()
                        : new ArrayList<>()
        );

        product.setIs24HourDelivery(
                req.getIs24HourDelivery() != null
                        ? req.getIs24HourDelivery()
                        : false
        );

        product.setRating(
                req.getRating() != null
                        ? req.getRating()
                        : 4.5
        );

        product.setReviewCount(
                req.getReviewCount() != null
                        ? req.getReviewCount()
                        : 0
        );

        product.setGstRate(
                req.getGstRate() != null
                        ? req.getGstRate()
                        : BigDecimal.valueOf(18.0)
        );

        product.setHsnCode(
                req.getHsnCode()
        );

        product.setSpecifications(
                req.getSpecifications() != null
                        ? req.getSpecifications()
                        : new java.util.LinkedHashMap<>()
        );

        product.setBulkPricingTiers(
                req.getBulkPricingTiers() != null
                        ? req.getBulkPricingTiers()
                        : new ArrayList<>()
        );

        product.setVendor(
                req.getVendor()
        );
    }

    private ProductResponse mapToResponse(
            Product p
    ) {

        Integer categoryId =
                p.getSubcategory() != null
                        && p.getSubcategory().getCategory() != null
                        ? p.getSubcategory()
                        .getCategory()
                        .getCategoryId()
                        : null;

        ApprovalStatus approvalStatus =
                p.getApprovalStatus() != null
                        ? p.getApprovalStatus()
                        : ApprovalStatus.PENDING;

        String status;

        if (approvalStatus
                == ApprovalStatus.REJECTED) {

            status = "REJECTED";

        } else if (approvalStatus
                == ApprovalStatus.PENDING) {

            status = "PENDING";

        } else {

            status = Boolean.TRUE.equals(
                    p.getActive()
            )
                    ? "APPROVED"
                    : "INACTIVE";
        }

        return ProductResponse.builder()
                .productId(p.getProductId())
                .subcategoryId(
                        p.getSubcategory() != null
                                ? p.getSubcategory()
                                .getSubcategoryId()
                                : null
                )
                .categoryId(categoryId)
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
                .active(
                        Boolean.TRUE.equals(
                                p.getActive()
                        )
                )
                .is24HourDelivery(
                        p.is24HourDelivery()
                )
                .rating(p.getRating())
                .reviewCount(p.getReviewCount())
                .gstRate(p.getGstRate())
                .hsnCode(p.getHsnCode())
                .specifications(p.getSpecifications())
                .bulkPricingTiers(
                        p.getBulkPricingTiers()
                )
                .vendor(p.getVendor())
                .approvalStatus(
                        approvalStatus.name()
                )
                .status(status)
                .rejectionReason(
                        p.getRejectionReason()
                )
                .createdAt(p.getCreatedAt())
                .build();
    }
}