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
public class SubcategoryServiceImpl implements SubcategoryService {

    private final SubcategoryRepository repository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final S3ImageService s3ImageService;

    @Override
    public SubcategoryResponse create(SubcategoryRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        String slug = generateSlug(request.getName(), request.getSlug());
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw new ResourceConflictException("Subcategory already exists with slug: " + slug);
        }

        Subcategory subcategory = Subcategory.builder()
                .category(category)
                .name(request.getName())
                .slug(slug)
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
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
        List<Subcategory> list;

        // Correctly apply categoryId and active filters (Fixing Bug 1)
        if (categoryId != null) {
            if (Boolean.TRUE.equals(active)) {
                list = repository.findByCategory_CategoryIdAndActiveOrderBySortOrderAsc(categoryId, true);
            } else {
                list = repository.findByCategory_CategoryIdOrderBySortOrderAsc(categoryId);
            }
        } else if (Boolean.TRUE.equals(active)) {
            list = repository.findByActiveTrueOrderBySortOrderAsc();
        } else {
            list = repository.findAllByOrderBySortOrderAsc();
        }

        return list.stream().map(this::mapToResponse).toList();
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

        String slug = generateSlug(request.getName(), request.getSlug());
        if (repository.existsBySlugIgnoreCaseAndSubcategoryIdNot(slug, id)) {
            throw new ResourceConflictException("Subcategory already exists with slug: " + slug);
        }

        subcategory.setName(request.getName());
        subcategory.setSlug(slug);
        if (request.getImageUrl() != null) {
            subcategory.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            subcategory.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            subcategory.setSortOrder(request.getSortOrder());
        }

        Subcategory saved = repository.save(subcategory);

        if (request.getImageUrl() != null && oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(request.getImageUrl())) {
            s3ImageService.deleteImage(oldImageUrl);
        }

        return mapToResponse(saved);
    }

    @Override
    public void delete(Integer id) {
        Subcategory subcategory = findSubcategory(id);
        String imageUrl = subcategory.getImageUrl();
        repository.delete(subcategory);

        if (imageUrl != null && !imageUrl.isBlank()) {
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
                .sortOrder(s.getSortOrder())
                .productCount(count)
                .createdAt(s.getCreatedAt())
                .build();
    }
}