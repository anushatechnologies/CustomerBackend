package com.example.project.customer.service;

import com.example.project.customer.entity.CategorySpecification;
import com.example.project.customer.entity.Specification;
import com.example.project.customer.entity.SpecificationOption;
import com.example.project.customer.repository.CategorySpecificationRepository;
import com.example.project.customer.repository.SpecificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSpecificationValidator {

    private final CategorySpecificationRepository categorySpecificationRepository;
    private final SpecificationRepository specificationRepository;

    /**
     * Validates and normalizes product specifications against the category's available specification pool.
     *
     * Rules:
     * 1. Category specifications represent the AVAILABLE POOL (e.g. 50 specifications).
     * 2. Product specifications store ONLY the SELECTED SUBSET and their values.
     * 3. Unused/unselected optional specifications are NOT stored (and empty/null entries are omitted).
     * 4. All keys in the returned map are normalized to the canonical specification "key" (e.g. "conductor_material", "grade").
     * 5. If category has no specification mappings, backward compatibility is preserved (unmapped products pass).
     *
     * @param categoryId     the target category ID
     * @param specifications the map of specification name/key to value
     * @return a normalized LinkedHashMap with canonical specification keys and valid values
     */
    public Map<String, String> validateAndNormalize(Integer categoryId, Map<String, String> specifications) {
        if (categoryId == null) {
            log.debug("No categoryId provided for specification validation; skipping category mapping checks");
            return cleanLegacyMap(specifications);
        }

        List<CategorySpecification> activeMappings = categorySpecificationRepository
                .findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(categoryId);

        // Backward compatibility: If no specifications are configured for this category, do not block
        if (activeMappings.isEmpty()) {
            log.debug("Category id={} has no specification mappings configured; skipping validation", categoryId);
            return cleanLegacyMap(specifications);
        }

        // 1. Enforce all required specifications
        for (CategorySpecification mapping : activeMappings) {
            Specification spec = mapping.getSpecification();
            if (Boolean.TRUE.equals(mapping.getRequired()) && Boolean.TRUE.equals(spec.getActive())) {
                boolean hasValue = false;
                if (specifications != null) {
                    for (Map.Entry<String, String> entry : specifications.entrySet()) {
                        if (entry.getKey() != null && matches(spec, entry.getKey())) {
                            if (entry.getValue() != null && !entry.getValue().trim().isBlank()) {
                                hasValue = true;
                                break;
                            }
                        }
                    }
                }
                if (!hasValue) {
                    throw new IllegalArgumentException("Specification '" + spec.getName() + "' is required for this category");
                }
            }
        }

        if (specifications == null || specifications.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 2. Validate supplied specifications and construct normalized subset
        Map<String, String> normalizedMap = new LinkedHashMap<>();
        Set<Integer> matchedSpecIds = new HashSet<>();

        for (Map.Entry<String, String> entry : specifications.entrySet()) {
            String inputKey = entry.getKey();
            String value = entry.getValue();

            if (inputKey == null || inputKey.trim().isBlank()) {
                throw new IllegalArgumentException("Specification key cannot be empty");
            }
            String trimmedKey = inputKey.trim();

            CategorySpecification matchedMapping = null;
            for (CategorySpecification cs : activeMappings) {
                if (matches(cs.getSpecification(), trimmedKey)) {
                    matchedMapping = cs;
                    break;
                }
            }

            if (matchedMapping == null) {
                // Check if specification exists elsewhere to provide precise error message
                Optional<Specification> globalSpec = specificationRepository.findByKeyIgnoreCase(trimmedKey)
                        .or(() -> specificationRepository.findByNameIgnoreCase(trimmedKey));

                if (globalSpec.isEmpty()) {
                    throw new IllegalArgumentException("Specification '" + trimmedKey + "' does not exist");
                } else if (!Boolean.TRUE.equals(globalSpec.get().getActive())) {
                    throw new IllegalArgumentException("Specification '" + globalSpec.get().getName() + "' is inactive and cannot be selected");
                } else {
                    throw new IllegalArgumentException("Specification '" + globalSpec.get().getName() + "' does not belong to the selected category");
                }
            }

            Specification spec = matchedMapping.getSpecification();

            if (!Boolean.TRUE.equals(spec.getActive())) {
                throw new IllegalArgumentException("Specification '" + spec.getName() + "' is inactive and cannot be selected");
            }

            // Prevent duplicate specification keys (e.g. sending both 'Grade' and 'grade')
            if (!matchedSpecIds.add(spec.getSpecificationId())) {
                throw new IllegalArgumentException("Duplicate specification provided: '" + spec.getName() + "'");
            }

            // If an optional specification is sent with null or blank value, do NOT store it
            if (value == null || value.trim().isBlank()) {
                if (Boolean.TRUE.equals(matchedMapping.getRequired())) {
                    throw new IllegalArgumentException("Specification '" + spec.getName() + "' is required for this category");
                }
                // Skip empty optional specification
                continue;
            }

            String trimmedValue = value.trim();

            // 3. Validate value based on inputType
            validateValue(spec, trimmedValue);

            // 4. Store using canonical specification KEY (e.g. "conductor_material", "grade")
            String storageKey = (spec.getKey() != null && !spec.getKey().isBlank())
                    ? spec.getKey()
                    : spec.getName();

            normalizedMap.put(storageKey, trimmedValue);
        }

        return normalizedMap;
    }

    /**
     * Backward-compatible validator method.
     */
    public void validateProductSpecifications(Integer categoryId, Map<String, String> specifications) {
        validateAndNormalize(categoryId, specifications);
    }

    private Map<String, String> cleanLegacyMap(Map<String, String> specifications) {
        if (specifications == null) {
            return new LinkedHashMap<>();
        }
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : specifications.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank()
                    && entry.getValue() != null && !entry.getValue().isBlank()) {
                cleaned.put(entry.getKey().trim(), entry.getValue().trim());
            }
        }
        return cleaned;
    }

    private boolean matches(Specification spec, String inputKey) {
        if (inputKey == null) return false;
        String trimmed = inputKey.trim();
        return spec.getName().equalsIgnoreCase(trimmed)
                || (spec.getKey() != null && spec.getKey().equalsIgnoreCase(trimmed));
    }

    private void validateValue(Specification spec, String value) {
        if (spec.getInputType() == null) {
            return;
        }

        switch (spec.getInputType()) {
            case DROPDOWN -> {
                List<String> allowed = getOptionValues(spec);
                boolean matched = allowed.stream().anyMatch(opt -> opt.equalsIgnoreCase(value));
                if (!matched) {
                    throw new IllegalArgumentException("Invalid value '" + value + "' for specification '"
                            + spec.getName() + "'. Allowed options: " + allowed);
                }
            }
            case MULTI_SELECT -> {
                List<String> allowed = getOptionValues(spec);
                String[] parts = value.split(",");
                for (String part : parts) {
                    String cleanPart = part.trim();
                    if (!cleanPart.isEmpty()) {
                        boolean matched = allowed.stream().anyMatch(opt -> opt.equalsIgnoreCase(cleanPart));
                        if (!matched) {
                            throw new IllegalArgumentException("Invalid value '" + cleanPart + "' for multi-select specification '"
                                    + spec.getName() + "'. Allowed options: " + allowed);
                        }
                    }
                }
            }
            case NUMBER -> {
                try {
                    new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Specification '" + spec.getName() + "' must be a valid number, got: '" + value + "'");
                }
            }
            case BOOLEAN -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Specification '" + spec.getName() + "' must be a boolean ('true' or 'false'), got: '" + value + "'");
                }
            }
            case TEXT -> {
                // Any non-null text value is valid
            }
        }
    }

    private List<String> getOptionValues(Specification spec) {
        if (spec.getOptions() == null) {
            return List.of();
        }
        return spec.getOptions().stream()
                .map(SpecificationOption::getOptionValue)
                .toList();
    }
}
