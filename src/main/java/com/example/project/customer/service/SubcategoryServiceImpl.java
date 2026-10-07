package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SubcategoryServiceImpl implements SubcategoryService {

    private final SubcategoryRepository repository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final S3ImageService s3ImageService;

    @Override
    public SubcategoryResponse create(SubcategoryRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String cleanName = request.getName() != null ? request.getName().trim() : "";
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Subcategory name cannot be empty");
        }

        if (repository.existsByNameIgnoreCaseAndCategory_CategoryId(cleanName, category.getCategoryId())) {
            throw new ResourceConflictException("Subcategory already exists with name: '" + cleanName + "' in category: '" + category.getName() + "'");
        }

        String slug = generateSlug(cleanName, request.getSlug());
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw new ResourceConflictException("Subcategory already exists with slug: '" + slug + "'");
        }

        String validImageUrl = com.example.project.customer.validation.ImageUrlValidator.validateForSave(request.getImageUrl());

        Subcategory subcategory = Subcategory.builder()
                .category(category)
                .name(cleanName)
                .slug(slug)
                .imageUrl(validImageUrl)
                .active(request.getActive() != null ? request.getActive() : true)
                .visibleOnWebsite(request.getVisibleOnWebsite() != null ? request.getVisibleOnWebsite() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .productCount(0)
                .build();

        return mapToResponse(repository.save(subcategory));
    }

    @Override
    @Transactional(readOnly = true)
    public SubcategoryResponse getById(Integer id) {
        Subcategory subcategory = findSubcategory(id);
        return mapToResponse(subcategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getAll(Integer categoryId, Boolean active) {
        return getAll(categoryId, active, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getAll(Integer categoryId, Boolean active, Boolean visibleOnWebsite) {
        List<Subcategory> list;

        if (categoryId != null) {
            if (Boolean.TRUE.equals(visibleOnWebsite)) {
                // New website visibility rule: isActive = true AND visibleOnWebsite = true
                boolean act = active == null || Boolean.TRUE.equals(active);
                list = repository.findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(categoryId, act, true);
            } else if (Boolean.FALSE.equals(visibleOnWebsite)) {
                if (active != null) {
                    list = repository.findByCategory_CategoryIdAndActiveAndVisibleOnWebsiteOrderBySortOrderAsc(categoryId, active, false);
                } else {
                    list = repository.findByCategory_CategoryIdAndVisibleOnWebsiteOrderBySortOrderAsc(categoryId, false);
                }
            } else if (Boolean.TRUE.equals(active)) {
                // Existing App rule: isActive = true, visibleOnWebsite is NOT filtered
                list = repository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(categoryId, true);
            } else if (Boolean.FALSE.equals(active)) {
                list = repository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(categoryId, false);
            } else {
                list = repository.findByCategory_CategoryIdOrderBySortOrderAsc(categoryId);
            }
        } else if (Boolean.TRUE.equals(visibleOnWebsite)) {
            // New website visibility rule without category filter: isActive = true AND visibleOnWebsite = true
            boolean act = active == null || Boolean.TRUE.equals(active);
            list = repository.findByActiveAndVisibleOnWebsiteOrderBySortOrderAsc(act, true);
        } else if (Boolean.FALSE.equals(visibleOnWebsite)) {
            if (active != null) {
                list = repository.findByActiveAndVisibleOnWebsiteOrderBySortOrderAsc(active, false);
            } else {
                list = repository.findByVisibleOnWebsiteOrderBySortOrderAsc(false);
            }
        } else if (Boolean.TRUE.equals(active)) {
            // Existing App rule: active = true, visibleOnWebsite is NOT filtered
            list = repository.findByActiveTrueOrderBySortOrderAsc();
        } else {
            list = repository.findAllByOrderBySortOrderAsc();
        }

        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getWebsiteSubcategories(Integer categoryId) {
        return getAll(categoryId, true, true);
    }

    @Override
    public SubcategoryResponse update(Integer id, SubcategoryRequest request) {
        Subcategory subcategory = findSubcategory(id);
        String oldImageUrl = subcategory.getImageUrl();

        if (request.getCategoryId() != null && !request.getCategoryId().equals(subcategory.getCategory().getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            subcategory.setCategory(category);
        }

        String cleanName = request.getName() != null ? request.getName().trim() : "";
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Subcategory name cannot be empty");
        }

        if (repository.existsByNameIgnoreCaseAndCategory_CategoryIdAndSubcategoryIdNot(cleanName, subcategory.getCategory().getCategoryId(), id)) {
            throw new ResourceConflictException("Subcategory already exists with name: '" + cleanName + "' in category: '" + subcategory.getCategory().getName() + "'");
        }

        String slug = generateSlug(cleanName, request.getSlug());
        if (repository.existsBySlugIgnoreCaseAndSubcategoryIdNot(slug, id)) {
            throw new ResourceConflictException("Subcategory already exists with slug: '" + slug + "'");
        }

        subcategory.setName(cleanName);
        subcategory.setSlug(slug);
        if (request.getImageUrl() != null) {
            String validImageUrl = com.example.project.customer.validation.ImageUrlValidator.validateForSave(request.getImageUrl());
            subcategory.setImageUrl(validImageUrl);
        }
        if (request.getActive() != null) {
            subcategory.setActive(request.getActive());
        }
        if (request.getVisibleOnWebsite() != null) {
            subcategory.setVisibleOnWebsite(request.getVisibleOnWebsite());
        }
        if (request.getSortOrder() != null) {
            subcategory.setSortOrder(request.getSortOrder());
        }

        Subcategory saved = repository.save(subcategory);

        if (request.getImageUrl() != null && oldImageUrl != null && !oldImageUrl.isBlank()
                && !oldImageUrl.equals(subcategory.getImageUrl())
                && !com.example.project.customer.validation.ImageUrlValidator.isBlobOrDataUrl(oldImageUrl)) {
            s3ImageService.deleteImage(oldImageUrl);
        }

        return mapToResponse(saved);
    }

    @Override
    public SubcategoryResponse updateWebsiteVisibility(Integer id, Boolean visibleOnWebsite) {
        Subcategory subcategory = findSubcategory(id);
        subcategory.setVisibleOnWebsite(visibleOnWebsite != null ? visibleOnWebsite : true);
        return mapToResponse(repository.save(subcategory));
    }

    @Override
    public void delete(Integer id) {
        Subcategory subcategory = findSubcategory(id);
        String imageUrl = subcategory.getImageUrl();
        repository.delete(subcategory);

        if (imageUrl != null && !imageUrl.isBlank()
                && !com.example.project.customer.validation.ImageUrlValidator.isBlobOrDataUrl(imageUrl)) {
            s3ImageService.deleteImage(imageUrl);
        }
    }

    private Subcategory findSubcategory(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + id));
    }

    private String generateSlug(String name, String providedSlug) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return providedSlug.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-");
        }
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private SubcategoryResponse mapToResponse(Subcategory s) {
        int count = productRepository.countByBrand_Subcategory_SubcategoryId(s.getSubcategoryId());
        return SubcategoryResponse.builder()
                .subcategoryId(s.getSubcategoryId())
                .categoryId(s.getCategory().getCategoryId())
                .name(s.getName())
                .slug(s.getSlug())
                .imageUrl(s.getImageUrl())
                .active(s.isActive())
                .visibleOnWebsite(s.isVisibleOnWebsite())
                .sortOrder(s.getSortOrder())
                .productCount(count)
                .createdAt(s.getCreatedAt())
                .build();
    }
}