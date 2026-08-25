package com.example.project.customer.service;

import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
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

    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsBySlugIgnoreCase(request.slug())) throw conflict(request.slug());
        return response(repository.save(apply(new Category(), request)));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Integer id) {
        return response(find(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = find(id);
        if (repository.existsBySlugIgnoreCaseAndCategoryIdNot(request.slug(), id)) throw conflict(request.slug());
        return response(repository.save(apply(category, request)));
    }

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

    private CategoryResponse response(Category c) {
        return new CategoryResponse(c.getCategoryId(), c.getName(), c.getSlug(), c.getImageUrl(),
                c.isActive(), c.getSortOrder(), c.getCreatedAt());
    }
}