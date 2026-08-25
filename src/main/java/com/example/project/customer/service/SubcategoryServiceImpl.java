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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubcategoryServiceImpl implements SubcategoryService {

    private final SubcategoryRepository repository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public SubcategoryServiceImpl(SubcategoryRepository repository,
                                  CategoryRepository categoryRepository,
                                  ProductRepository productRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public SubcategoryResponse create(SubcategoryRequest request) {
        if (repository.existsBySlugIgnoreCase(request.slug())) {
            throw conflict(request.slug());
        }
        Subcategory saved = repository.save(apply(new Subcategory(), request));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubcategoryResponse getById(Integer id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getAll(Integer categoryId, Boolean active) {
        List<Subcategory> list;
        if (categoryId != null && active != null) {
            list = repository.findByCategory_CategoryIdAndActive(categoryId, active);
        } else if (categoryId != null) {
            list = repository.findByCategory_CategoryId(categoryId);
        } else if (active != null) {
            list = repository.findByActiveOrderBySortOrderAsc(active);
        } else {
            list = repository.findAllByOrderBySortOrderAsc();
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    public SubcategoryResponse update(Integer id, SubcategoryRequest request) {
        Subcategory subcategory = find(id);
        if (repository.existsBySlugIgnoreCaseAndSubcategoryIdNot(request.slug(), id)) {
            throw conflict(request.slug());
        }
        return toResponse(repository.save(apply(subcategory, request)));
    }

    @Override
    public void delete(Integer id) {
        repository.delete(find(id));
    }

    private Subcategory find(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + id));
    }

    private ResourceConflictException conflict(String slug) {
        return new ResourceConflictException("Subcategory already exists with slug: " + slug);
    }

    private Subcategory apply(Subcategory item, SubcategoryRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
        item.setCategory(category);
        item.setName(request.name());
        item.setSlug(request.slug());
        item.setImageUrl(request.imageUrl());
        item.setActive(request.active());
        item.setSortOrder(request.sortOrder());
        return item;
    }

    private SubcategoryResponse toResponse(Subcategory s) {
        long productCount = productRepository.countBySubcategory_SubcategoryId(s.getSubcategoryId());
        return new SubcategoryResponse(
                s.getSubcategoryId(),
                s.getCategory() != null ? s.getCategory().getCategoryId() : null,
                s.getName(),
                s.getSlug(),
                s.getImageUrl(),
                s.isActive(),
                s.getSortOrder(),
                productCount,
                s.getCreatedAt()
        );
    }
}