package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.ProductListResponse;
import com.example.project.customer.dto.ProductRejectionRequest;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SearchSuggestionResponse;
import com.example.project.customer.dto.StockQuantityUpdateRequest;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.Map;

@Slf4j
@Service
@Transactional
@SuppressWarnings("null")
public class ProductServiceImpl implements ProductService {

        private final ProductRepository repository;
        private final BrandRepository brandRepository;
        private final SubcategoryRepository subcategoryRepository;
        private final CategoryRepository categoryRepository;
        private final StoreRepository storeRepository;
        private final S3ImageService s3ImageService;
        private final ProductTaxonomyService productTaxonomyService;

        public ProductServiceImpl(
                        ProductRepository repository,
                        BrandRepository brandRepository,
                        SubcategoryRepository subcategoryRepository,
                        CategoryRepository categoryRepository,
                        StoreRepository storeRepository,
                        S3ImageService s3ImageService) {
                this(repository, brandRepository, subcategoryRepository, categoryRepository, storeRepository, s3ImageService, null);
        }

        @Autowired
        public ProductServiceImpl(
                        ProductRepository repository,
                        BrandRepository brandRepository,
                        SubcategoryRepository subcategoryRepository,
                        CategoryRepository categoryRepository,
                        StoreRepository storeRepository,
                        S3ImageService s3ImageService,
                        ProductTaxonomyService productTaxonomyService) {
                this.repository = repository;
                this.brandRepository = brandRepository;
                this.subcategoryRepository = subcategoryRepository;
                this.categoryRepository = categoryRepository;
                this.storeRepository = storeRepository;
                this.s3ImageService = s3ImageService;
                this.productTaxonomyService = productTaxonomyService;
        }

        @Autowired(required = false)
        private OrderItemRepository orderItemRepository;

        @Autowired(required = false)
        private CartItemRepository cartItemRepository;

        @Autowired(required = false)
        private com.example.project.customer.repository.HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;

        @Autowired(required = false)
        private com.example.project.customer.config.PlatformMarketplaceProperties platformMarketplaceProperties;

        @Autowired(required = false)
        private ProductAvailabilityService productAvailabilityService;

        @Autowired(required = false)
        private com.example.project.customer.repository.SellerRepository sellerRepository;

        public void setProductAvailabilityService(ProductAvailabilityService productAvailabilityService) {
                this.productAvailabilityService = productAvailabilityService;
        }

        public void setHinchStoreVendorInventoryRepository(com.example.project.customer.repository.HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository) {
                this.hinchStoreVendorInventoryRepository = hinchStoreVendorInventoryRepository;
        }

        @Autowired(required = false)
        private InventorySynchronizationService inventorySynchronizationService;

        public void setInventorySynchronizationService(InventorySynchronizationService inventorySynchronizationService) {
                this.inventorySynchronizationService = inventorySynchronizationService;
        }

        public void setPlatformMarketplaceProperties(com.example.project.customer.config.PlatformMarketplaceProperties platformMarketplaceProperties) {
                this.platformMarketplaceProperties = platformMarketplaceProperties;
        }

        @Override
        @CacheEvict(value = "brands", allEntries = true)
        public ProductResponse create(ProductRequest request) {
                String cleanTitle = request.getTitle() != null ? request.getTitle().trim() : "";
                if (cleanTitle.isEmpty()) {
                        throw new IllegalArgumentException("Product title cannot be empty");
                }

                if (repository.existsByTitleIgnoreCase(cleanTitle)) {
                        throw new ResourceConflictException("Product already exists with title: '" + cleanTitle + "'");
                }

                if (request.getSku() != null && !request.getSku().isBlank()) {
                        String sku = request.getSku().trim().toUpperCase();
                        if (repository.existsBySkuIgnoreCase(sku)) {
                                throw new ResourceConflictException("Product already exists with SKU: '" + sku + "'");
                        }
                }

                String slug = generateSlug(cleanTitle, request.getSlug());
                if (repository.existsBySlugIgnoreCase(slug)) {
                        throw new ResourceConflictException("Product already exists with slug: '" + slug + "'");
                }

                Brand brand = resolveBrand(request);

                if (request.getSpecifications() != null) {
                        request.setSpecifications(cleanSpecifications(request.getSpecifications()));
                }

                Product product = new Product();
                mapRequestToProduct(product, request, brand);
                product.setTitle(cleanTitle);
                product.setSlug(slug);
                if (request.getSku() != null && !request.getSku().isBlank()) {
                        product.setSku(request.getSku().trim().toUpperCase());
                }

                if (product.getStore() == null) {
                        storeRepository.findById(1)
                                        .or(() -> storeRepository.findAll().stream()
                                                        .filter(s -> s.getStatus() == StoreStatus.ACTIVE)
                                                        .findFirst())
                                        .or(() -> storeRepository.findAll().stream().findFirst())
                                        .ifPresent(product::setStore);
                }

                // IMPORTANT:
                // Every newly submitted product must wait for admin approval.
                product.setApprovalStatus(ApprovalStatus.PENDING);
                product.setActive(false);
                product.setRejectionReason(null);

                Product saved = repository.save(product);
                if (inventorySynchronizationService != null) {
                        inventorySynchronizationService.syncProductInventory(saved, request.getStockQty(), null);
                }
                return enrichAvailability(mapToResponse(saved), saved);
        }

        @Override
        @Transactional(readOnly = true)
        public ProductResponse getById(Integer id) {
                Product product = repository
                                .findByProductIdAndApprovalStatusAndActive(
                                                id,
                                                ApprovalStatus.APPROVED,
                                                true)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found with id: " + id));

                return enrichAvailability(mapToResponse(product), product);
        }

        @Override
        @Transactional(readOnly = true)
        public ApiResponse<List<ProductResponse>> getAll(
                        Integer categoryId,
                        String category,
                        Integer subcategoryId,
                        Integer brandId,
                        String search,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        String brand,
                        Boolean is24HourDelivery,
                        String sort,
                        int page,
                        int limit) {

                int pageNumber = Math.max(page - 1, 0);
                int pageSize = limit > 0 ? limit : 20;

                Sort sorting = getSort(sort);

                Pageable pageable = PageRequest.of(
                                pageNumber,
                                pageSize,
                                sorting);

                Specification<Product> spec = (root, query, cb) -> {

                        List<Predicate> predicates = new ArrayList<>();

                        // Only approved + active products are visible to customers
                        predicates.add(
                                        cb.equal(
                                                        root.get("approvalStatus"),
                                                        ApprovalStatus.APPROVED));

                        predicates.add(
                                        cb.isTrue(root.get("active")));

                        // Explicit LEFT JOINs so that products with NULL subcategory_id or brand_id are NEVER dropped by implicit inner joins
                        jakarta.persistence.criteria.Join<Product, Subcategory> subJoin = root.join("subcategory", jakarta.persistence.criteria.JoinType.LEFT);
                        jakarta.persistence.criteria.Join<Subcategory, Category> subCatJoin = subJoin.join("category", jakarta.persistence.criteria.JoinType.LEFT);
                        jakarta.persistence.criteria.Join<Product, Brand> brandJoin = root.join("brand", jakarta.persistence.criteria.JoinType.LEFT);
                        jakarta.persistence.criteria.Join<Brand, Subcategory> brandSubJoin = brandJoin.join("subcategory", jakarta.persistence.criteria.JoinType.LEFT);
                        jakarta.persistence.criteria.Join<Subcategory, Category> brandCatJoin = brandSubJoin.join("category", jakarta.persistence.criteria.JoinType.LEFT);

                        // Category filter via direct subcategory or brand -> subcategory -> category
                        if (categoryId != null) {
                                Predicate directCat = cb.equal(subCatJoin.get("categoryId"), categoryId);
                                Predicate brandCat = cb.and(
                                                cb.isNull(root.get("subcategory")),
                                                cb.equal(brandCatJoin.get("categoryId"), categoryId));
                                predicates.add(cb.or(directCat, brandCat));
                        }

                        // Category filter via direct subcategory or brand -> subcategory -> category (Name or Slug, e.g.
                        // category=Interior)
                        if (category != null && !category.isBlank()) {
                                String catTrimmed = category.trim();
                                List<Predicate> catPredicates = new ArrayList<>();
                                catPredicates.add(cb.equal(cb.lower(subCatJoin.get("name")), catTrimmed.toLowerCase()));
                                catPredicates.add(cb.equal(cb.lower(subCatJoin.get("slug")), catTrimmed.toLowerCase()));

                                catPredicates.add(cb.and(
                                                cb.isNull(root.get("subcategory")),
                                                cb.equal(cb.lower(brandCatJoin.get("name")), catTrimmed.toLowerCase())));
                                catPredicates.add(cb.and(
                                                cb.isNull(root.get("subcategory")),
                                                cb.equal(cb.lower(brandCatJoin.get("slug")), catTrimmed.toLowerCase())));

                                try {
                                        int numId = Integer.parseInt(catTrimmed);
                                        catPredicates.add(cb.equal(subCatJoin.get("categoryId"), numId));
                                        catPredicates.add(cb.and(
                                                        cb.isNull(root.get("subcategory")),
                                                        cb.equal(brandCatJoin.get("categoryId"), numId)));
                                } catch (NumberFormatException ignored) {
                                }

                                predicates.add(cb.or(catPredicates.toArray(new Predicate[0])));
                        }

                        // Subcategory filter via direct subcategory or brand -> subcategory
                        if (subcategoryId != null) {
                                Predicate directSub = cb.equal(subJoin.get("subcategoryId"), subcategoryId);
                                Predicate brandSub = cb.and(
                                                cb.isNull(root.get("subcategory")),
                                                cb.equal(brandSubJoin.get("subcategoryId"), subcategoryId));
                                predicates.add(cb.or(directSub, brandSub));
                        }

                        // Brand ID filter
                        if (brandId != null) {
                                predicates.add(cb.equal(brandJoin.get("brandId"), brandId));
                        }

                        // Brand name string filter
                        if (brand != null && !brand.isBlank()) {
                                predicates.add(cb.equal(cb.lower(brandJoin.get("name")), brand.trim().toLowerCase()));
                        }

                        // Search filter
                        if (search != null && !search.isBlank()) {

                                String pattern = "%" + search.trim().toLowerCase() + "%";

                                Predicate titlePredicate = cb.like(
                                                cb.lower(root.get("title")),
                                                pattern);

                                Predicate brandPredicate = cb.like(
                                                cb.lower(brandJoin.get("name")),
                                                pattern);

                                Predicate descriptionPredicate = cb.like(
                                                cb.lower(root.get("description")),
                                                pattern);

                                predicates.add(
                                                cb.or(
                                                                titlePredicate,
                                                                brandPredicate,
                                                                descriptionPredicate));
                        }

                        // Minimum price
                        if (minPrice != null) {
                                predicates.add(
                                                cb.greaterThanOrEqualTo(
                                                                root.get("price"),
                                                                minPrice));
                        }

                        // Maximum price
                        if (maxPrice != null) {
                                predicates.add(
                                                cb.lessThanOrEqualTo(
                                                                root.get("price"),
                                                                maxPrice));
                        }

                        // 24-hour delivery
                        if (Boolean.TRUE.equals(is24HourDelivery)) {
                                predicates.add(
                                                cb.isTrue(
                                                                root.get("is24HourDelivery")));
                        }

                        return cb.and(
                                        predicates.toArray(new Predicate[0]));
                };

                Page<Product> productPage = repository.findAll(spec, pageable);

                List<ProductResponse> data = productPage.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList();

                if (productAvailabilityService != null) {
                        Map<Integer, com.example.project.customer.dto.ProductAvailability> availMap = productAvailabilityService.getAvailabilityBatch(productPage.getContent());
                        for (ProductResponse resp : data) {
                                com.example.project.customer.dto.ProductAvailability avail = availMap.get(resp.getProductId());
                                if (avail != null) {
                                        resp.setStockQty(avail.getAvailableQty());
                                        resp.setAvailableQty(avail.getAvailableQty());
                                        resp.setInStock(avail.isInStock());
                                        resp.setAvailabilityReason(avail.getReason());
                                }
                        }
                } else if (hinchStoreVendorInventoryRepository != null && platformMarketplaceProperties != null) {
                        List<Integer> platformProductIds = productPage.getContent().stream()
                                        .filter(p -> p.getStore() != null && platformMarketplaceProperties.isPlatformStore(p.getStore().getStoreId()))
                                        .map(Product::getProductId)
                                        .toList();
                        if (!platformProductIds.isEmpty()) {
                                List<Object[]> stockRows = hinchStoreVendorInventoryRepository.getAvailableStockForProducts(platformProductIds);
                                java.util.Map<Integer, Integer> stockMap = new java.util.HashMap<>();
                                for (Object[] row : stockRows) {
                                        if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                                                stockMap.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
                                        }
                                }
                                for (ProductResponse resp : data) {
                                        if (platformProductIds.contains(resp.getProductId())) {
                                                resp.setStockQty(stockMap.getOrDefault(resp.getProductId(), 0));
                                        }
                                }
                        }
                }

                PaginationMeta pagination = PaginationMeta.of(
                                page > 0 ? page : 1,
                                pageSize,
                                productPage.getTotalElements());

                return ApiResponse.paginated(
                                data,
                                pagination);
        }

        @Override
        @CacheEvict(value = "brands", allEntries = true)
        public ProductResponse update(
                        Integer id,
                        ProductRequest request) {

                Product product = findProduct(id);
                String oldMainImage = product.getImageUrl();
                List<String> oldGalleryImages = product.getImages() != null ? new ArrayList<>(product.getImages())
                                : List.of();

                String cleanTitle = request.getTitle() != null ? request.getTitle().trim() : "";
                if (cleanTitle.isEmpty()) {
                        throw new IllegalArgumentException("Product title cannot be empty");
                }

                if (repository.existsByTitleIgnoreCaseAndProductIdNot(cleanTitle, id)) {
                        throw new ResourceConflictException("Product already exists with title: '" + cleanTitle + "'");
                }

                if (request.getSku() != null && !request.getSku().isBlank()) {
                        String sku = request.getSku().trim().toUpperCase();
                        if (repository.existsBySkuIgnoreCaseAndProductIdNot(sku, id)) {
                                throw new ResourceConflictException("Product already exists with SKU: '" + sku + "'");
                        }
                }

                String slug = generateSlug(cleanTitle, request.getSlug());
                if (repository.existsBySlugIgnoreCaseAndProductIdNot(slug, id)) {
                        throw new ResourceConflictException("Product already exists with slug: '" + slug + "'");
                }

                Brand brand = resolveBrand(request);

                if (request.getSpecifications() != null) {
                        request.setSpecifications(cleanSpecifications(request.getSpecifications()));
                }

                mapRequestToProduct(
                                product,
                                request,
                                brand);
                product.setTitle(cleanTitle);
                product.setSlug(slug);
                if (request.getSku() != null && !request.getSku().isBlank()) {
                        product.setSku(request.getSku().trim().toUpperCase());
                }

                /*
                 * Do NOT allow a normal product update to approve
                 * or activate a pending/rejected product.
                 */
                if (product.getApprovalStatus() != ApprovalStatus.APPROVED) {
                        product.setActive(false);
                }

                Product saved = repository.save(product);
                if (request.getStockQty() != null && inventorySynchronizationService != null) {
                        inventorySynchronizationService.syncProductInventory(saved, request.getStockQty(), null);
                }

                // Clean up old main image if replaced and no longer in use
                String newMainImage = saved.getImageUrl();
                List<String> newGalleryImages = saved.getImages() != null ? saved.getImages() : List.of();

                if (oldMainImage != null && !oldMainImage.isBlank() && !oldMainImage.equals(newMainImage)
                                && !newGalleryImages.contains(oldMainImage)) {
                        s3ImageService.deleteImage(oldMainImage);
                }

                // Clean up old gallery images that are removed and not used as main image
                for (String oldImg : oldGalleryImages) {
                        if (oldImg != null && !oldImg.isBlank() && !newGalleryImages.contains(oldImg)
                                        && !oldImg.equals(newMainImage)) {
                                s3ImageService.deleteImage(oldImg);
                        }
                }

                return enrichAvailability(mapToResponse(saved), saved);
        }

        @Override
        public ProductResponse updateStockQuantity(Integer id, StockQuantityUpdateRequest request) {

                Product product = repository.findByIdForStockUpdate(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found with id: " + id));

                int currentStock = product.getStockQty() != null ? product.getStockQty() : 0;
                int incomingStock = request.getStockQty() != null ? request.getStockQty() : 0;
                int newTotal = Math.max(0, currentStock + incomingStock);

                product.setStockQty(newTotal);
                Product saved = repository.save(product);
                if (inventorySynchronizationService != null) {
                        inventorySynchronizationService.syncProductInventory(saved, newTotal, null);
                }

                return enrichAvailability(mapToResponse(saved), saved);
        }

        @Override
        @CacheEvict(value = "brands", allEntries = true)
        public void delete(Integer id) {

                Product product = findProduct(id);
                String mainImage = product.getImageUrl();
                List<String> galleryImages = product.getImages() != null ? new ArrayList<>(product.getImages())
                                : List.of();

                boolean hasOrderItems = orderItemRepository != null && orderItemRepository.existsByProductId(id);
                boolean hasCartItems = cartItemRepository != null && cartItemRepository.existsByProduct_ProductId(id);

                if (hasOrderItems || hasCartItems) {
                        // Soft-delete: deactivate & reject so historical orders and active carts remain
                        // structurally intact
                        product.setActive(false);
                        product.setApprovalStatus(ApprovalStatus.REJECTED);
                        product.setRejectionReason("Soft-deleted by admin (order/cart history preserved)");
                        product.setUpdatedAt(java.time.LocalDateTime.now());
                        repository.save(product);
                        log.info("Soft-deleted product id={} due to active references (orders={}, carts={})",
                                        id, hasOrderItems, hasCartItems);
                } else {
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
        }

        // =========================================================
        // SEARCH SUGGESTIONS
        // =========================================================

        @Override
        @Transactional(readOnly = true)
        public SearchSuggestionResponse getSearchSuggestions(
                        String query) {

                String trimmed = query != null ? query.trim() : "";

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
                                                                        "JSW Sheets"))
                                        .build();
                }

                List<Product> products = repository
                                .findTop5ApprovedActiveSuggestions(
                                                trimmed,
                                                PageRequest.of(0, 5));

                List<SearchSuggestionResponse.ProductSuggestion> productSuggestions = products.stream()
                                .map(product -> SearchSuggestionResponse.ProductSuggestion
                                                .builder()
                                                .productId(product.getProductId())
                                                .title(product.getTitle())
                                                .category(
                                                                product.getBrand() != null
                                                                                && product.getBrand()
                                                                                                .getSubcategory() != null
                                                                                && product.getBrand().getSubcategory()
                                                                                                .getCategory() != null
                                                                                                                ? product.getBrand()
                                                                                                                                .getSubcategory()
                                                                                                                                .getCategory()
                                                                                                                                .getName()
                                                                                                                : "")
                                                .build())
                                .toList();

                List<Category> categories = categoryRepository
                                .findByActiveTrueOrderBySortOrderAsc()
                                .stream()
                                .filter(category -> category.getName()
                                                .toLowerCase()
                                                .contains(
                                                                trimmed.toLowerCase()))
                                .limit(5)
                                .toList();

                List<SearchSuggestionResponse.CategorySuggestion> categorySuggestions = categories.stream()
                                .map(category -> SearchSuggestionResponse.CategorySuggestion
                                                .builder()
                                                .categoryId(
                                                                category.getCategoryId())
                                                .name(category.getName())
                                                .build())
                                .toList();

                return SearchSuggestionResponse.builder()
                                .products(productSuggestions)
                                .categories(categorySuggestions)
                                .popularSearches(
                                                List.of(
                                                                "UltraTech Cement",
                                                                "Tata Tiscon 12mm",
                                                                "Armoured Cable",
                                                                "CPVC Pipes"))
                                .build();
        }

        // =========================================================
        // ADMIN PRODUCT MANAGEMENT
        // =========================================================

        @Override
        @CacheEvict(value = "brands", allEntries = true)
        public ProductResponse activate(Integer id) {

                Product product = findProduct(id);

                if (product.getApprovalStatus() != ApprovalStatus.APPROVED) {

                        throw new ResourceConflictException(
                                        "Product cannot be activated because it is not approved.");
                }

                product.setActive(true);
                Product saved = repository.save(product);

                return enrichAvailability(mapToResponse(saved), saved);
        }

        @Override
        @CacheEvict(value = "brands", allEntries = true)
        public ProductResponse deactivate(Integer id) {

                Product product = findProduct(id);

                product.setActive(false);
                Product saved = repository.save(product);

                return enrichAvailability(mapToResponse(saved), saved);
        }

        @Override
        @Transactional(readOnly = true)
        public ProductListResponse getPending() {

                List<Product> products = repository
                                .findByApprovalStatus(
                                                ApprovalStatus.PENDING);

                List<ProductResponse> responses = products.stream()
                                .map(this::mapToResponse)
                                .toList();

                if (productAvailabilityService != null && !products.isEmpty()) {
                        Map<Integer, com.example.project.customer.dto.ProductAvailability> availMap = productAvailabilityService.getAvailabilityBatch(products);
                        for (ProductResponse resp : responses) {
                                com.example.project.customer.dto.ProductAvailability pa = availMap.get(resp.getProductId());
                                if (pa != null) {
                                        resp.setStockQty(pa.getAvailableQty());
                                        resp.setAvailableQty(pa.getAvailableQty());
                                        resp.setInStock(pa.isInStock());
                                        resp.setAvailabilityReason(pa.getReason());
                                }
                        }
                }

                return new ProductListResponse(
                                responses,
                                responses.size());
        }

        private boolean isSellerApproved(Seller seller) {
                if (seller == null)
                        return true;
                return seller.getVerificationStatus() == com.example.project.customer.entity.VerificationStatus.VERIFIED;
        }

        @Override
        @Transactional(readOnly = true)
        public ProductListResponse getAdminAll() {

                List<Product> products = repository.findAll();
                List<ProductResponse> responses = products.stream()
                                .map(this::mapToResponse)
                                .toList();

                if (productAvailabilityService != null && !products.isEmpty()) {
                        Map<Integer, com.example.project.customer.dto.ProductAvailability> availMap = productAvailabilityService.getAvailabilityBatch(products);
                        for (ProductResponse resp : responses) {
                                com.example.project.customer.dto.ProductAvailability pa = availMap.get(resp.getProductId());
                                if (pa != null) {
                                        resp.setStockQty(pa.getAvailableQty());
                                        resp.setAvailableQty(pa.getAvailableQty());
                                        resp.setInStock(pa.isInStock());
                                        resp.setAvailabilityReason(pa.getReason());
                                }
                        }
                }

                return new ProductListResponse(
                                responses,
                                responses.size());
        }

        @Override
        @Transactional(readOnly = true)
        public ProductResponse getAdminById(Integer id) {
                Product product = findProduct(id);
                return enrichAvailability(mapToResponse(product), product);
        }

        @Override
        public ProductResponse approve(Integer id) {

                Product product = findProduct(id);

                if (product.getSeller() != null && !isSellerApproved(product.getSeller())) {
                        String sellerName = product.getSeller().getCompanyName() != null
                                        ? product.getSeller().getCompanyName()
                                        : (product.getSeller().getName() != null ? product.getSeller().getName()
                                                        : "Seller #" + product.getSeller().getSellerId());
                        throw new IllegalStateException("Cannot approve product because the seller '" + sellerName +
                                        "' (ID: " + product.getSeller().getSellerId()
                                        + ") is not yet verified and approved by admin.");
                }

                product.setApprovalStatus(
                                ApprovalStatus.APPROVED);

                product.setRejectionReason(null);

                // Approval makes the product visible to customers.
                product.setActive(true);

                Product saved = repository.save(product);
                if (inventorySynchronizationService != null && saved.getStockQty() != null && saved.getStockQty() > 0) {
                        inventorySynchronizationService.syncProductInventory(saved, saved.getStockQty(), saved.getSeller() != null ? saved.getSeller().getSellerId() : null);
                }

                return enrichAvailability(mapToResponse(saved), saved);
        }

        @Override
        public ProductResponse reject(
                        Integer id,
                        ProductRejectionRequest request) {

                Product product = findProduct(id);

                product.setApprovalStatus(
                                ApprovalStatus.REJECTED);

                product.setActive(false);

                product.setRejectionReason(
                                request.reason());

                return mapToResponse(
                                repository.save(product));
        }

        // =========================================================
        // HELPERS
        // =========================================================

        private Brand resolveBrand(ProductRequest request) {
                if (request.getBrandId() != null) {
                        return brandRepository.findById(request.getBrandId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Brand not found with id: " + request.getBrandId()));
                }

                if (request.getBrand() != null && !request.getBrand().isBlank()) {
                        if (request.getSubcategoryId() != null) {
                                return brandRepository
                                                .findByNameIgnoreCaseAndSubcategory_SubcategoryId(
                                                                request.getBrand().trim(), request.getSubcategoryId())
                                                .orElseGet(() -> brandRepository
                                                                .findByNameIgnoreCase(request.getBrand().trim())
                                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                                "Brand not found with name: "
                                                                                                + request.getBrand())));
                        }
                        return brandRepository.findByNameIgnoreCase(request.getBrand().trim())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Brand not found with name: " + request.getBrand()));
                }

                throw new IllegalArgumentException("Brand ID or Brand name is required");
        }

        private Product findProduct(Integer id) {

                return repository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Product not found with id: " + id));
        }

        private Sort getSort(String sort) {

                if ("price_asc".equalsIgnoreCase(sort)) {

                        return Sort.by(
                                        Sort.Direction.ASC,
                                        "price");

                } else if ("price_desc".equalsIgnoreCase(sort)) {

                        return Sort.by(
                                        Sort.Direction.DESC,
                                        "price");

                } else if ("rating".equalsIgnoreCase(sort)) {

                        return Sort.by(
                                        Sort.Direction.DESC,
                                        "rating");

                } else {

                        return Sort.by(
                                        Sort.Direction.DESC,
                                        "createdAt");
                }
        }

        private void mapRequestToProduct(
                        Product product,
                        ProductRequest req,
                        Brand brand) {

                product.setBrand(brand);

                if (productTaxonomyService != null) {
                        Subcategory subcategory = productTaxonomyService.resolveSubcategoryForProduct(
                                        req.getSubcategoryId(),
                                        req.getCategoryId(),
                                        product.getProductId() != null ? product : null,
                                        brand);
                        product.setSubcategory(subcategory);
                } else if (req.getSubcategoryId() != null && subcategoryRepository != null) {
                        product.setSubcategory(subcategoryRepository.findById(req.getSubcategoryId()).orElse(null));
                }

                product.setTitle(req.getTitle());

                if (req.getSlug() != null
                                && !req.getSlug().isBlank()) {

                        product.setSlug(
                                        req.getSlug().trim());

                } else if (product.getSlug() == null) {

                        product.setSlug(
                                        req.getTitle()
                                                        .toLowerCase()
                                                        .replaceAll(
                                                                        "[^a-z0-9]+",
                                                                        "-")
                                                        .replaceAll(
                                                                        "^-|-$",
                                                                        ""));
                }

                product.setSku(req.getSku());

                product.setDescription(
                                req.getDescription());

                if (req.getMrp() != null && req.getPrice() != null && req.getMrp().compareTo(req.getPrice()) < 0) {
                    throw new IllegalArgumentException("Selling price cannot exceed MRP (Maximum Retail Price).");
                }
                product.setPrice(
                                req.getPrice());

                product.setMrp(
                                req.getMrp());

                product.setStockQty(
                                req.getStockQty());

                product.setUnit(
                                req.getUnit());

                product.setMoq(
                                req.getMoq() != null
                                                ? req.getMoq()
                                                : 1);

                product.setImageUrl(
                                req.getImageUrl());

                product.setImages(
                                req.getImages() != null
                                                ? req.getImages()
                                                : new ArrayList<>());

                product.setIs24HourDelivery(
                                req.getIs24HourDelivery() != null
                                                ? req.getIs24HourDelivery()
                                                : false);

                product.setRating(
                                req.getRating() != null
                                                ? req.getRating()
                                                : (product.getRating() != null ? product.getRating() : 0.0));

                product.setReviewCount(
                                req.getReviewCount() != null
                                                ? req.getReviewCount()
                                                : (product.getReviewCount() != null ? product.getReviewCount() : 0));

                product.setGstRate(
                                req.getGstRate() != null
                                                ? req.getGstRate()
                                                : BigDecimal.valueOf(18.0));

                product.setHsnCode(
                                req.getHsnCode());

                product.setSpecifications(cleanSpecifications(req.getSpecifications()));

                product.setBulkPricingTiers(
                                req.getBulkPricingTiers() != null
                                                ? req.getBulkPricingTiers()
                                                : new ArrayList<>());
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

        @Override
        public ProductResponse mapToResponse(
                        Product p) {

                Integer brandId = p.getBrand() != null ? p.getBrand().getBrandId() : null;
                String brandName = p.getBrand() != null ? p.getBrand().getName() : null;

                Subcategory subcategory = p.getSubcategory();
                Integer subcategoryId = subcategory != null ? subcategory.getSubcategoryId() : null;
                String subcategoryName = subcategory != null ? subcategory.getName() : null;

                Category category = p.getCategory();
                Integer categoryId = category != null ? category.getCategoryId() : null;
                String categoryName = category != null ? category.getName() : null;

                ApprovalStatus approvalStatus = p.getApprovalStatus() != null
                                ? p.getApprovalStatus()
                                : ApprovalStatus.PENDING;

                String status;

                if (approvalStatus == ApprovalStatus.REJECTED) {

                        status = "REJECTED";

                } else if (approvalStatus == ApprovalStatus.PENDING) {

                        status = "PENDING";

                } else {

                        status = Boolean.TRUE.equals(
                                        p.getActive())
                                                        ? "APPROVED"
                                                        : "INACTIVE";
                }

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
                                .images(p.getImages())
                                .price(p.getPrice())
                                .mrp(p.getMrp())
                                .unit(p.getUnit())
                                .moq(p.getMoq())
                                .stockQty(p.getStockQty())
                                .availableQty(p.getStockQty() != null ? p.getStockQty() : 0)
                                .inStock(Boolean.TRUE.equals(p.getActive()) && p.getStockQty() != null && p.getStockQty() > 0)
                                .availabilityReason(Boolean.FALSE.equals(p.getActive()) ? "PRODUCT_INACTIVE" : (p.getStockQty() == null || p.getStockQty() <= 0 ? "OUT_OF_STOCK" : null))
                                .active(
                                                Boolean.TRUE.equals(
                                                                p.getActive()))
                                .is24HourDelivery(
                                                p.is24HourDelivery())
                                .rating(p.getRating())
                                .reviewCount(p.getReviewCount())
                                .gstRate(p.getGstRate())
                                .hsnCode(p.getHsnCode())
                                .specifications(p.getSpecifications())
                                .bulkPricingTiers(
                                                p.getBulkPricingTiers())
                                .sellerId(
                                                p.getSeller() != null ? p.getSeller().getSellerId() : null)
                                .sellerName(
                                                p.getSeller() != null ? p.getSeller().getName() : null)
                                .approvalStatus(
                                                approvalStatus.name())
                                .status(status)
                                .rejectionReason(
                                                p.getRejectionReason())
                                .sellingPrice(
                                                p.getSellingPrice() != null ? p.getSellingPrice() : p.getPrice())
                                .createdAt(p.getCreatedAt())
                                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                                .build();
        }

        private String generateSlug(String name, String providedSlug) {
                if (providedSlug != null && !providedSlug.isBlank()) {
                        return providedSlug.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-");
                }
                return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        }

        private boolean isPlatformProduct(Product product) {
                if (product == null || product.getStore() == null) {
                        return false;
                }
                if (platformMarketplaceProperties == null) {
                        return Integer.valueOf(1).equals(product.getStore().getStoreId());
                }
                return platformMarketplaceProperties.isPlatformStore(product.getStore().getStoreId());
        }

        private ProductResponse enrichAvailability(ProductResponse resp, Product product) {
                if (resp == null || product == null) {
                        return resp;
                }
                if (productAvailabilityService != null) {
                        com.example.project.customer.dto.ProductAvailability avail = productAvailabilityService.getAvailability(product);
                        resp.setStockQty(avail.getAvailableQty());
                        resp.setAvailableQty(avail.getAvailableQty());
                        resp.setInStock(avail.isInStock());
                        resp.setAvailabilityReason(avail.getReason());
                }
                return resp;
        }
}
