package com.example.project.customer.service;

import com.example.project.customer.dto.CategorySpecificationRequest;
import com.example.project.customer.dto.CategorySpecificationResponse;
import com.example.project.customer.dto.CategorySpecificationUpdateRequest;
import com.example.project.customer.dto.SpecificationOptionRequest;
import com.example.project.customer.dto.SpecificationOptionResponse;
import com.example.project.customer.dto.SpecificationRequest;
import com.example.project.customer.dto.SpecificationResponse;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CategorySpecification;
import com.example.project.customer.entity.Specification;
import com.example.project.customer.entity.SpecificationOption;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CategorySpecificationRepository;
import com.example.project.customer.repository.SpecificationOptionRepository;
import com.example.project.customer.repository.SpecificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificationServiceImpl implements SpecificationService {

    private final SpecificationRepository specificationRepository;
    private final SpecificationOptionRepository specificationOptionRepository;
    private final CategorySpecificationRepository categorySpecificationRepository;
    private final CategoryRepository categoryRepository;

    // =========================================================
    // SPECIFICATION MASTER
    // =========================================================

    @Override
    @Transactional
    public SpecificationResponse create(SpecificationRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Specification name cannot be empty");
        }

        String key = resolveKey(request.getKey(), name);
        if (specificationRepository.existsByKeyIgnoreCase(key)) {
            throw new ResourceConflictException("Specification already exists with key: '" + key + "'");
        }

        Specification spec = Specification.builder()
                .name(name)
                .key(key)
                .inputType(request.getInputType())
                .unit(request.getUnit() != null ? request.getUnit().trim() : null)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDateTime.now())
                .build();

        Specification saved = specificationRepository.save(spec);

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            int order = 1;
            for (String optVal : request.getOptions()) {
                if (optVal != null && !optVal.isBlank()) {
                    SpecificationOption opt = SpecificationOption.builder()
                            .specification(saved)
                            .optionValue(optVal.trim())
                            .displayOrder(order++)
                            .createdAt(LocalDateTime.now())
                            .build();
                    specificationOptionRepository.save(opt);
                }
            }
        }

        return mapToResponse(specificationRepository.findById(saved.getSpecificationId()).orElse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecificationResponse> getAll(Boolean active, String search) {
        List<Specification> list;
        if (Boolean.TRUE.equals(active)) {
            list = specificationRepository.findByActiveTrueOrderByNameAsc();
        } else {
            list = specificationRepository.findAllByOrderByNameAsc();
        }

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            list = list.stream()
                    .filter(s -> s.getName().toLowerCase().contains(query) || s.getKey().toLowerCase().contains(query))
                    .toList();
        }

        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpecificationResponse getById(Integer id) {
        Specification spec = findSpecification(id);
        return mapToResponse(spec);
    }

    @Override
    @Transactional
    public SpecificationResponse update(Integer id, SpecificationRequest request) {
        Specification spec = findSpecification(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            spec.setName(request.getName().trim());
        }

        if (request.getKey() != null && !request.getKey().isBlank()) {
            String key = request.getKey().trim();
            if (specificationRepository.existsByKeyIgnoreCaseAndSpecificationIdNot(key, id)) {
                throw new ResourceConflictException("Specification already exists with key: '" + key + "'");
            }
            spec.setKey(key);
        }

        if (request.getInputType() != null) {
            spec.setInputType(request.getInputType());
        }

        if (request.getUnit() != null) {
            spec.setUnit(request.getUnit().trim());
        }

        if (request.getActive() != null) {
            spec.setActive(request.getActive());
        }

        spec.setUpdatedAt(LocalDateTime.now());
        Specification saved = specificationRepository.save(spec);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Specification spec = findSpecification(id);
        spec.setActive(false);
        spec.setUpdatedAt(LocalDateTime.now());
        specificationRepository.save(spec);
    }

    // =========================================================
    // SPECIFICATION OPTIONS
    // =========================================================

    @Override
    @Transactional
    public SpecificationOptionResponse addOption(Integer specificationId, SpecificationOptionRequest request) {
        Specification spec = findSpecification(specificationId);
        String val = request.getValue() != null ? request.getValue().trim() : "";
        if (val.isEmpty()) {
            throw new IllegalArgumentException("Option value cannot be empty");
        }

        SpecificationOption option = SpecificationOption.builder()
                .specification(spec)
                .optionValue(val)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .build();

        SpecificationOption saved = specificationOptionRepository.save(option);
        return mapOptionToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecificationOptionResponse> getOptions(Integer specificationId) {
        findSpecification(specificationId);
        return specificationOptionRepository
                .findBySpecification_SpecificationIdOrderByDisplayOrderAscOptionIdAsc(specificationId)
                .stream()
                .map(this::mapOptionToResponse)
                .toList();
    }

    @Override
    @Transactional
    public SpecificationOptionResponse updateOption(Integer specificationId, Integer optionId, SpecificationOptionRequest request) {
        findSpecification(specificationId);
        SpecificationOption option = specificationOptionRepository
                .findBySpecification_SpecificationIdAndOptionId(specificationId, optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option not found with id: " + optionId + " for specification: " + specificationId));

        if (request.getValue() != null && !request.getValue().isBlank()) {
            option.setOptionValue(request.getValue().trim());
        }
        if (request.getDisplayOrder() != null) {
            option.setDisplayOrder(request.getDisplayOrder());
        }

        return mapOptionToResponse(specificationOptionRepository.save(option));
    }

    @Override
    @Transactional
    public void deleteOption(Integer specificationId, Integer optionId) {
        findSpecification(specificationId);
        SpecificationOption option = specificationOptionRepository
                .findBySpecification_SpecificationIdAndOptionId(specificationId, optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option not found with id: " + optionId + " for specification: " + specificationId));

        specificationOptionRepository.delete(option);
    }

    // =========================================================
    // CATEGORY SPECIFICATION MAPPING
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<CategorySpecificationResponse> getCategorySpecifications(Integer categoryId, Boolean activeOnly) {
        findCategory(categoryId);
        List<CategorySpecification> mappings;
        if (Boolean.TRUE.equals(activeOnly)) {
            mappings = categorySpecificationRepository
                    .findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(categoryId);
        } else {
            mappings = categorySpecificationRepository
                    .findByCategory_CategoryIdOrderByDisplayOrderAsc(categoryId);
        }

        return mappings.stream()
                .map(this::mapCategorySpecToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategorySpecificationResponse addCategorySpecification(Integer categoryId, CategorySpecificationRequest request) {
        Category category = findCategory(categoryId);
        Specification specification = findSpecification(request.getSpecificationId());

        if (categorySpecificationRepository.existsByCategory_CategoryIdAndSpecification_SpecificationId(categoryId, request.getSpecificationId())) {
            throw new ResourceConflictException("Specification '" + specification.getName() + "' is already mapped to category '" + category.getName() + "'");
        }

        CategorySpecification mapping = CategorySpecification.builder()
                .category(category)
                .specification(specification)
                .required(request.getRequired() != null ? request.getRequired() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDateTime.now())
                .build();

        CategorySpecification saved = categorySpecificationRepository.save(mapping);
        return mapCategorySpecToResponse(saved);
    }

    @Override
    @Transactional
    public CategorySpecificationResponse updateCategorySpecification(Integer categoryId, Integer specificationId, CategorySpecificationUpdateRequest request) {
        findCategory(categoryId);
        CategorySpecification mapping = categorySpecificationRepository
                .findByCategory_CategoryIdAndSpecification_SpecificationId(categoryId, specificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Mapping not found for category " + categoryId + " and specification " + specificationId));

        if (request.getRequired() != null) {
            mapping.setRequired(request.getRequired());
        }
        if (request.getDisplayOrder() != null) {
            mapping.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            mapping.setActive(request.getActive());
        }

        mapping.setUpdatedAt(LocalDateTime.now());
        CategorySpecification saved = categorySpecificationRepository.save(mapping);
        return mapCategorySpecToResponse(saved);
    }

    @Override
    @Transactional
    public void removeCategorySpecification(Integer categoryId, Integer specificationId) {
        findCategory(categoryId);
        CategorySpecification mapping = categorySpecificationRepository
                .findByCategory_CategoryIdAndSpecification_SpecificationId(categoryId, specificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Mapping not found for category " + categoryId + " and specification " + specificationId));

        categorySpecificationRepository.delete(mapping);
    }

    @Override
    @Transactional
    public List<CategorySpecificationResponse> batchConfigureCategorySpecifications(Integer categoryId, List<CategorySpecificationRequest> requests) {
        Category category = findCategory(categoryId);

        if (requests == null) {
            return getCategorySpecifications(categoryId, false);
        }

        List<CategorySpecificationResponse> results = new ArrayList<>();
        for (CategorySpecificationRequest req : requests) {
            if (req.getSpecificationId() == null) {
                continue;
            }
            Specification spec = findSpecification(req.getSpecificationId());
            CategorySpecification mapping = categorySpecificationRepository
                    .findByCategory_CategoryIdAndSpecification_SpecificationId(categoryId, req.getSpecificationId())
                    .orElseGet(() -> CategorySpecification.builder()
                            .category(category)
                            .specification(spec)
                            .createdAt(LocalDateTime.now())
                            .build());

            mapping.setRequired(req.getRequired() != null ? req.getRequired() : false);
            mapping.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
            mapping.setActive(req.getActive() != null ? req.getActive() : true);
            mapping.setUpdatedAt(LocalDateTime.now());

            results.add(mapCategorySpecToResponse(categorySpecificationRepository.save(mapping)));
        }

        return results;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Specification findSpecification(Integer id) {
        return specificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specification not found with id: " + id));
    }

    private Category findCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private String resolveKey(String explicitKey, String name) {
        if (explicitKey != null && !explicitKey.isBlank()) {
            return explicitKey.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
        }
        return name.toLowerCase().trim().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private SpecificationResponse mapToResponse(Specification s) {
        List<SpecificationOptionResponse> optionDtos = s.getOptions() != null
                ? s.getOptions().stream().map(this::mapOptionToResponse).toList()
                : List.of();

        return SpecificationResponse.builder()
                .id(s.getSpecificationId())
                .name(s.getName())
                .key(s.getKey())
                .inputType(s.getInputType())
                .unit(s.getUnit())
                .active(s.isActive())
                .options(optionDtos)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private SpecificationOptionResponse mapOptionToResponse(SpecificationOption o) {
        return SpecificationOptionResponse.builder()
                .id(o.getOptionId())
                .value(o.getOptionValue())
                .displayOrder(o.getDisplayOrder())
                .build();
    }

    private CategorySpecificationResponse mapCategorySpecToResponse(CategorySpecification cs) {
        Specification s = cs.getSpecification();
        List<String> optionStrings = s.getOptions() != null
                ? s.getOptions().stream().map(SpecificationOption::getOptionValue).toList()
                : List.of();

        return CategorySpecificationResponse.builder()
                .id(s.getSpecificationId())
                .mappingId(cs.getId())
                .categoryId(cs.getCategory().getCategoryId())
                .name(s.getName())
                .key(s.getKey())
                .inputType(s.getInputType())
                .unit(s.getUnit())
                .required(cs.isRequired())
                .displayOrder(cs.getDisplayOrder())
                .active(cs.isActive() && s.isActive())
                .options(optionStrings)
                .build();
    }
}
