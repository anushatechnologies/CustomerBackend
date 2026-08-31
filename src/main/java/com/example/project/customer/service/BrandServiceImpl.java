package com.example.project.customer.service;

import com.example.project.customer.dto.BrandRequest;
import com.example.project.customer.dto.BrandResponse;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.BrandRepository;
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
public class BrandServiceImpl implements BrandService {

    private final BrandRepository repository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;
    private final S3ImageService s3ImageService;

    @Override
    public BrandResponse create(BrandRequest request) {
        Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + request.getSubcategoryId()));

        String slug = generateSlug(request.getName(), request.getSlug());
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw new ResourceConflictException("Brand already exists with slug: " + slug);
        }

        Brand brand = Brand.builder()
                .subcategory(subcategory)
                .name(request.getName())
                .slug(slug)
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .productCount(0)
                .build();

        return mapToResponse(repository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(Integer id) {
        Brand brand = findBrand(id);
        return mapToResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAll(Integer categoryId, Integer subcategoryId, Boolean active) {
        List<Brand> list;

        if (subcategoryId != null) {
            if (Boolean.TRUE.equals(active)) {
                list = repository.findBySubcategory_SubcategoryIdAndActiveOrderBySortOrderAsc(subcategoryId, true);
            } else {
                list = repository.findBySubcategory_SubcategoryIdOrderBySortOrderAsc(subcategoryId);
            }
        } else if (categoryId != null) {
            if (Boolean.TRUE.equals(active)) {
                list = repository.findBySubcategory_Category_CategoryIdAndActiveOrderBySortOrderAsc(categoryId, true);
            } else {
                list = repository.findBySubcategory_Category_CategoryIdOrderBySortOrderAsc(categoryId);
            }
        } else if (Boolean.TRUE.equals(active)) {
            list = repository.findByActiveTrueOrderBySortOrderAsc();
        } else {
            list = repository.findAllByOrderBySortOrderAsc();
        }

        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    public BrandResponse update(Integer id, BrandRequest request) {
        Brand brand = findBrand(id);
        String oldImageUrl = brand.getImageUrl();

        if (request.getSubcategoryId() != null && !request.getSubcategoryId().equals(brand.getSubcategory().getSubcategoryId())) {
            Subcategory subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found with id: " + request.getSubcategoryId()));
            brand.setSubcategory(subcategory);
        }

        String slug = generateSlug(request.getName(), request.getSlug());
        if (repository.existsBySlugIgnoreCaseAndBrandIdNot(slug, id)) {
            throw new ResourceConflictException("Brand already exists with slug: " + slug);
        }

        brand.setName(request.getName());
        brand.setSlug(slug);
        if (request.getImageUrl() != null) {
            brand.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            brand.setSortOrder(request.getSortOrder());
        }

        Brand saved = repository.save(brand);

        if (request.getImageUrl() != null && oldImageUrl != null && !oldImageUrl.isBlank() && !oldImageUrl.equals(request.getImageUrl())) {
            s3ImageService.deleteImage(oldImageUrl);
        }

        return mapToResponse(saved);
    }

    @Override
    public void delete(Integer id) {
        Brand brand = findBrand(id);
        String imageUrl = brand.getImageUrl();
        repository.delete(brand);

        if (imageUrl != null && !imageUrl.isBlank()) {
            s3ImageService.deleteImage(imageUrl);
        }
    }

    private Brand findBrand(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    private String generateSlug(String name, String providedSlug) {
        if (providedSlug != null && !providedSlug.isBlank()) {
            return providedSlug.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-");
        }
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private BrandResponse mapToResponse(Brand b) {
        int count = productRepository.countByBrand_BrandId(b.getBrandId());
        String subcategoryName = b.getSubcategory() != null ? b.getSubcategory().getName() : null;
        Integer categoryId = b.getSubcategory() != null && b.getSubcategory().getCategory() != null
                ? b.getSubcategory().getCategory().getCategoryId() : null;
        String categoryName = b.getSubcategory() != null && b.getSubcategory().getCategory() != null
                ? b.getSubcategory().getCategory().getName() : null;

        return BrandResponse.builder()
                .brandId(b.getBrandId())
                .subcategoryId(b.getSubcategory() != null ? b.getSubcategory().getSubcategoryId() : null)
                .subcategoryName(subcategoryName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .name(b.getName())
                .slug(b.getSlug())
                .imageUrl(b.getImageUrl())
                .active(b.isActive())
                .sortOrder(b.getSortOrder())
                .productCount(count)
                .createdAt(b.getCreatedAt())
                .build();
    }
}
