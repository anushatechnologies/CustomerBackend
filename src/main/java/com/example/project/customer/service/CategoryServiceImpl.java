package com.example.project.customer.service;

import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
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
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;
    private final S3ImageService s3ImageService;

    @Override
    public CategoryResponse create(CategoryRequest request) {
        String slug = generateSlug(request.getName(), request.getSlug());
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw new ResourceConflictException("Category already exists with slug: " + slug);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .productCount(0)
                .build();

        return mapToResponse(repository.save(category), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Integer id) {
        Category category = findCategory(id);
        return mapToResponse(category, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(Boolean active, Boolean includeSubcategories) {
        List<Category> categories;
        if (Boolean.TRUE.equals(active)) {
            categories = repository.findByActiveTrueOrderBySortOrderAsc();
        } else {
            categories = repository.findAllByOrderBySortOrderAsc();
        }

        boolean includeSubs = Boolean.TRUE.equals(includeSubcategories);
        return categories.stream()
                .map(cat -> mapToResponse(cat, includeSubs))
                .toList();
    }

    @Override
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = findCategory(id);
        String oldImageUrl = category.getImageUrl();
        String slug = generateSlug(request.getName(), request.getSlug());
        if (repository.existsBySlugIgnoreCaseAndCategoryIdNot(slug, id)) {
            throw new ResourceConflictException("Category already exists with slug: " + slug);
        }

        category.setName(request.getName());
        category.setSlug(slug);
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        Category saved = repository.save(category);

        if (request.getImageUrl() != null && oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(request.getImageUrl())) {
            s3ImageService.deleteImage(oldImageUrl);
        }

        return mapToResponse(saved, false);
    }

    @Override
    public void delete(Integer id) {
        Category category = findCategory(id);
        String imageUrl = category.getImageUrl();
        repository.delete(category);

        if (imageUrl != null && !imageUrl.isBlank()) {
            s3ImageService.deleteImage(imageUrl);
        }
    }

    private Category findCategory(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private String generateSlug(String name, String providedSlug) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return providedSlug.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-");
        }
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private CategoryResponse mapToResponse(Category category, boolean includeSubcategories) {
        int count = productRepository.countBySubcategory_Category_CategoryId(category.getCategoryId());

        List<SubcategoryResponse> subs = null;
        if (includeSubcategories) {
            List<Subcategory> subcategories = subcategoryRepository.findByCategory_CategoryIdOrderBySortOrderAsc(category.getCategoryId());
            subs = subcategories.stream()
                    .map(s -> {
                        int sCount = productRepository.countBySubcategory_SubcategoryId(s.getSubcategoryId());
                        return SubcategoryResponse.builder()
                                .subcategoryId(s.getSubcategoryId())
                                .categoryId(category.getCategoryId())
                                .name(s.getName())
                                .slug(s.getSlug())
                                .imageUrl(s.getImageUrl())
                                .active(s.isActive())
                                .sortOrder(s.getSortOrder())
                                .productCount(sCount)
                                .createdAt(s.getCreatedAt())
                                .build();
                    })
                    .toList();
        }

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .active(category.isActive())
                .sortOrder(category.getSortOrder())
                .productCount(count)
                .subcategories(subs)
                .createdAt(category.getCreatedAt())
                .build();
    }
}