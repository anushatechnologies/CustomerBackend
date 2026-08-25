package com.example.project.customer.service;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
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

    public SubcategoryResponse create(SubcategoryRequest request) {
        if (repository.existsBySlugIgnoreCase(request.slug())) throw conflict(request.slug());
        return response(repository.save(apply(new Subcategory(), request)));
    }

    @Transactional(readOnly = true)
    public SubcategoryResponse getById(Integer id) {
        return response(find(id));
    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getAll() {
        return repository.findAll().stream().map(this::response).toList();
    }

    public SubcategoryResponse update(Integer id, SubcategoryRequest request) {
        Subcategory subcategory = find(id);
        if (repository.existsBySlugIgnoreCaseAndSubcategoryIdNot(request.slug(), id)) throw conflict(request.slug());
        return response(repository.save(apply(subcategory, request)));
    }

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
        item.setCategory(categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId())));
        item.setName(request.name());
        item.setSlug(request.slug());
        item.setImageUrl(request.imageUrl());
        item.setActive(request.active());
        item.setSortOrder(request.sortOrder());
        return item;
    }

    private SubcategoryResponse response(Subcategory s) {
        return new SubcategoryResponse(s.getSubcategoryId(), s.getCategory().getCategoryId(), s.getName(),
                s.getSlug(), s.getImageUrl(), s.isActive(), s.getSortOrder(), s.getCreatedAt());
    }
}