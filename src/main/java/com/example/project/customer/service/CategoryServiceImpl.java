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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository repository,
                               SubcategoryRepository subcategoryRepository,
                               ProductRepository productRepository) {
        this.repository = repository;
        this.subcategoryRepository = subcategoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsBySlugIgnoreCase(request.slug())) {
            throw conflict(request.slug());
        }
        Category saved = repository.save(apply(new Category(), request));
        return toResponse(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Integer id) {
        return toResponse(find(id), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(Boolean active, Boolean includeSubcategories) {
        List<Category> list;
        if (active != null) {
            list = repository.findByActiveOrderBySortOrderAsc(active);
        } else {
            list = repository.findAllByOrderBySortOrderAsc();
        }
        boolean withSub = Boolean.TRUE.equals(includeSubcategories);
        return list.stream().map(c -> toResponse(c, withSub)).toList();
    }

    @Override
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = find(id);
        if (repository.existsBySlugIgnoreCaseAndCategoryIdNot(request.slug(), id)) {
            throw conflict(request.slug());
        }
        return toResponse(repository.save(apply(category, request)), false);
    }

    @Override
    public void delete(Integer id) {
        repository.delete(find(id));
    }

    private Category find(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private ResourceConflictException conflict(String slug) {
        return new ResourceConflictException("Category already exists with slug: " + slug);
    }

    private Category apply(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setImageUrl(request.imageUrl());
        category.setActive(request.active());
        category.setSortOrder(request.sortOrder());
        return category;
    }

    private CategoryResponse toResponse(Category c, boolean includeSubcategories) {
        long productCount = productRepository.countByCategory_CategoryId(c.getCategoryId());
        List<SubcategoryResponse> subResp = null;
        if (includeSubcategories) {
            List<Subcategory> subs = subcategoryRepository.findByCategory_CategoryId(c.getCategoryId());
            subResp = subs.stream().map(s -> {
                long count = productRepository.countBySubcategory_SubcategoryId(s.getSubcategoryId());
                return new SubcategoryResponse(
                        s.getSubcategoryId(),
                        c.getCategoryId(),
                        s.getName(),
                        s.getSlug(),
                        s.getImageUrl(),
                        s.isActive(),
                        s.getSortOrder(),
                        count,
                        s.getCreatedAt()
                );
            }).toList();
        }

        return new CategoryResponse(
                c.getCategoryId(),
                c.getName(),
                c.getSlug(),
                c.getImageUrl(),
                c.isActive(),
                c.getSortOrder(),
                productCount,
                subResp,
                c.getCreatedAt()
        );
    }
}