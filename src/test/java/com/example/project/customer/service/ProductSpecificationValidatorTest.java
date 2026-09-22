package com.example.project.customer.service;

import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CategorySpecification;
import com.example.project.customer.entity.Specification;
import com.example.project.customer.entity.SpecificationInputType;
import com.example.project.customer.entity.SpecificationOption;
import com.example.project.customer.repository.CategorySpecificationRepository;
import com.example.project.customer.repository.SpecificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductSpecificationValidatorTest {

    @Mock
    private CategorySpecificationRepository categorySpecificationRepository;

    @Mock
    private SpecificationRepository specificationRepository;

    @InjectMocks
    private ProductSpecificationValidator validator;

    private Category category;
    private Specification gradeSpec;
    private Specification diameterSpec;
    private Specification isCertifiedSpec;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(10)
                .name("TMT Steel")
                .build();

        gradeSpec = Specification.builder()
                .specificationId(1)
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .active(true)
                .options(new ArrayList<>())
                .build();
        gradeSpec.getOptions().add(SpecificationOption.builder().specification(gradeSpec).optionValue("Fe 415").build());
        gradeSpec.getOptions().add(SpecificationOption.builder().specification(gradeSpec).optionValue("Fe 500").build());
        gradeSpec.getOptions().add(SpecificationOption.builder().specification(gradeSpec).optionValue("Fe 550D").build());

        diameterSpec = Specification.builder()
                .specificationId(2)
                .name("Diameter")
                .key("diameter")
                .inputType(SpecificationInputType.NUMBER)
                .unit("mm")
                .active(true)
                .build();

        isCertifiedSpec = Specification.builder()
                .specificationId(3)
                .name("ISI Certified")
                .key("isi_certified")
                .inputType(SpecificationInputType.BOOLEAN)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Backward compatibility: Category with no mappings allows any specifications")
    void validate_CategoryWithoutMappings_Passes() {
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(99))
                .thenReturn(List.of());

        Map<String, String> specs = Map.of("Any Custom Spec", "Any Value");

        assertThatCode(() -> validator.validateProductSpecifications(99, specs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Backward compatibility: Null categoryId passes without exception")
    void validate_NullCategoryId_Passes() {
        Map<String, String> specs = Map.of("Grade", "Fe 550D");

        assertThatCode(() -> validator.validateProductSpecifications(null, specs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Required specification missing throws IllegalArgumentException")
    void validate_MissingRequiredSpecification_ThrowsException() {
        CategorySpecification csRequired = CategorySpecification.builder()
                .category(category)
                .specification(gradeSpec)
                .required(true)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csRequired));

        Map<String, String> specs = new LinkedHashMap<>();
        // Grade is missing

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specification 'Grade' is required");
    }

    @Test
    @DisplayName("Valid required and optional specifications pass")
    void validate_ValidSpecifications_Passes() {
        CategorySpecification csGrade = CategorySpecification.builder()
                .category(category)
                .specification(gradeSpec)
                .required(true)
                .active(true)
                .build();

        CategorySpecification csDiameter = CategorySpecification.builder()
                .category(category)
                .specification(diameterSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csGrade, csDiameter));

        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Grade", "Fe 550D");
        specs.put("Diameter", "12.5");

        assertThatCode(() -> validator.validateProductSpecifications(10, specs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Invalid dropdown option throws IllegalArgumentException")
    void validate_InvalidDropdownOption_ThrowsException() {
        CategorySpecification csGrade = CategorySpecification.builder()
                .category(category)
                .specification(gradeSpec)
                .required(true)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csGrade));

        Map<String, String> specs = Map.of("Grade", "Fe Invalid Grade");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid value 'Fe Invalid Grade' for specification 'Grade'");
    }

    @Test
    @DisplayName("Invalid number format throws IllegalArgumentException")
    void validate_InvalidNumber_ThrowsException() {
        CategorySpecification csDiameter = CategorySpecification.builder()
                .category(category)
                .specification(diameterSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csDiameter));

        Map<String, String> specs = Map.of("Diameter", "twelve mm");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a valid number");
    }

    @Test
    @DisplayName("Invalid boolean value throws IllegalArgumentException")
    void validate_InvalidBoolean_ThrowsException() {
        CategorySpecification csCertified = CategorySpecification.builder()
                .category(category)
                .specification(isCertifiedSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csCertified));

        Map<String, String> specs = Map.of("ISI Certified", "yes");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a boolean ('true' or 'false')");
    }

    @Test
    @DisplayName("Unmapped specification rejected when category has mappings")
    void validate_UnmappedSpecification_ThrowsException() {
        CategorySpecification csGrade = CategorySpecification.builder()
                .category(category)
                .specification(gradeSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csGrade));

        Specification unmappedSpec = Specification.builder()
                .specificationId(99)
                .name("Tensile Strength")
                .key("tensile_strength")
                .active(true)
                .build();

        when(specificationRepository.findByKeyIgnoreCase("Tensile Strength")).thenReturn(Optional.empty());
        when(specificationRepository.findByNameIgnoreCase("Tensile Strength")).thenReturn(Optional.of(unmappedSpec));

        Map<String, String> specs = Map.of("Tensile Strength", "600");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the selected category");
    }

    @Test
    @DisplayName("Non-existent specification rejected")
    void validate_NonExistentSpecification_ThrowsException() {
        CategorySpecification csGrade = CategorySpecification.builder()
                .category(category)
                .specification(gradeSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csGrade));

        when(specificationRepository.findByKeyIgnoreCase("Alien Spec")).thenReturn(Optional.empty());
        when(specificationRepository.findByNameIgnoreCase("Alien Spec")).thenReturn(Optional.empty());

        Map<String, String> specs = Map.of("Alien Spec", "Unknown");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("Duplicate specification key provided throws IllegalArgumentException")
    void validate_DuplicateSpecificationKeys_ThrowsException() {
        CategorySpecification csGrade = CategorySpecification.builder()
                .category(category)
                .specification(gradeSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csGrade));

        // Sending both "Grade" and "grade" which resolve to the same specification
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Grade", "Fe 500");
        specs.put("grade", "Fe 550D");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate specification provided");
    }

    @Test
    @DisplayName("Inactive specification cannot be selected")
    void validate_InactiveSpecification_ThrowsException() {
        Specification inactiveSpec = Specification.builder()
                .specificationId(4)
                .name("Obsolete Spec")
                .key("obsolete_spec")
                .active(false)
                .build();

        CategorySpecification csInactive = CategorySpecification.builder()
                .category(category)
                .specification(inactiveSpec)
                .required(false)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(csInactive));

        Map<String, String> specs = Map.of("Obsolete Spec", "Any");

        assertThatThrownBy(() -> validator.validateProductSpecifications(10, specs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is inactive and cannot be selected");
    }
}
