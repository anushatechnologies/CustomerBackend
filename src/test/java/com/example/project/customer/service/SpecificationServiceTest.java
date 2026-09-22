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
import com.example.project.customer.entity.SpecificationInputType;
import com.example.project.customer.entity.SpecificationOption;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CategorySpecificationRepository;
import com.example.project.customer.repository.SpecificationOptionRepository;
import com.example.project.customer.repository.SpecificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SpecificationServiceTest {

    @Mock
    private SpecificationRepository specificationRepository;

    @Mock
    private SpecificationOptionRepository specificationOptionRepository;

    @Mock
    private CategorySpecificationRepository categorySpecificationRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SpecificationServiceImpl specificationService;

    private Specification gradeSpec;
    private Category tmtCategory;

    @BeforeEach
    void setUp() {
        gradeSpec = Specification.builder()
                .specificationId(1)
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .unit(null)
                .active(true)
                .options(new ArrayList<>())
                .build();

        tmtCategory = Category.builder()
                .categoryId(10)
                .name("TMT Steel & Rebars")
                .slug("tmt-steel-rebars")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Create specification successfully with options")
    void createSpecification_Success() {
        SpecificationRequest request = SpecificationRequest.builder()
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .options(List.of("Fe 415", "Fe 500", "Fe 550D"))
                .build();

        when(specificationRepository.existsByKeyIgnoreCase("grade")).thenReturn(false);
        when(specificationRepository.save(any(Specification.class))).thenReturn(gradeSpec);
        when(specificationRepository.findById(1)).thenReturn(Optional.of(gradeSpec));

        SpecificationResponse response = specificationService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Grade");
        assertThat(response.getKey()).isEqualTo("grade");
        verify(specificationRepository).save(any(Specification.class));
    }

    @Test
    @DisplayName("Create specification rejects duplicate key")
    void createSpecification_DuplicateKey_ThrowsConflict() {
        SpecificationRequest request = SpecificationRequest.builder()
                .name("Grade")
                .key("grade")
                .inputType(SpecificationInputType.DROPDOWN)
                .build();

        when(specificationRepository.existsByKeyIgnoreCase("grade")).thenReturn(true);

        assertThatThrownBy(() -> specificationService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists with key");
    }

    @Test
    @DisplayName("Get specification by ID successfully")
    void getById_Success() {
        when(specificationRepository.findById(1)).thenReturn(Optional.of(gradeSpec));

        SpecificationResponse response = specificationService.getById(1);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("Grade");
    }

    @Test
    @DisplayName("Get specification by non-existent ID throws 404")
    void getById_NotFound() {
        when(specificationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.getById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Add option to specification successfully")
    void addOption_Success() {
        when(specificationRepository.findById(1)).thenReturn(Optional.of(gradeSpec));
        SpecificationOption option = SpecificationOption.builder()
                .optionId(101)
                .specification(gradeSpec)
                .optionValue("Fe 550D")
                .displayOrder(1)
                .build();
        when(specificationOptionRepository.save(any(SpecificationOption.class))).thenReturn(option);

        SpecificationOptionRequest req = SpecificationOptionRequest.builder()
                .value("Fe 550D")
                .displayOrder(1)
                .build();

        SpecificationOptionResponse resp = specificationService.addOption(1, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getValue()).isEqualTo("Fe 550D");
        assertThat(resp.getId()).isEqualTo(101);
    }

    @Test
    @DisplayName("Map specification to category successfully")
    void addCategorySpecification_Success() {
        when(categoryRepository.findById(10)).thenReturn(Optional.of(tmtCategory));
        when(specificationRepository.findById(1)).thenReturn(Optional.of(gradeSpec));
        when(categorySpecificationRepository.existsByCategory_CategoryIdAndSpecification_SpecificationId(10, 1))
                .thenReturn(false);

        CategorySpecification mapping = CategorySpecification.builder()
                .id(50)
                .category(tmtCategory)
                .specification(gradeSpec)
                .required(true)
                .displayOrder(1)
                .active(true)
                .build();

        when(categorySpecificationRepository.save(any(CategorySpecification.class))).thenReturn(mapping);

        CategorySpecificationRequest req = CategorySpecificationRequest.builder()
                .specificationId(1)
                .required(true)
                .displayOrder(1)
                .build();

        CategorySpecificationResponse resp = specificationService.addCategorySpecification(10, req);

        assertThat(resp).isNotNull();
        assertThat(resp.getCategoryId()).isEqualTo(10);
        assertThat(resp.getName()).isEqualTo("Grade");
        assertThat(resp.getRequired()).isTrue();
    }

    @Test
    @DisplayName("Map duplicate specification to category throws ResourceConflictException")
    void addCategorySpecification_Duplicate_ThrowsConflict() {
        when(categoryRepository.findById(10)).thenReturn(Optional.of(tmtCategory));
        when(specificationRepository.findById(1)).thenReturn(Optional.of(gradeSpec));
        when(categorySpecificationRepository.existsByCategory_CategoryIdAndSpecification_SpecificationId(10, 1))
                .thenReturn(true);

        CategorySpecificationRequest req = CategorySpecificationRequest.builder()
                .specificationId(1)
                .required(true)
                .build();

        assertThatThrownBy(() -> specificationService.addCategorySpecification(10, req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already mapped");
    }

    @Test
    @DisplayName("Fetch category specifications ordered by displayOrder")
    void getCategorySpecifications_Ordered() {
        when(categoryRepository.findById(10)).thenReturn(Optional.of(tmtCategory));

        CategorySpecification mapping = CategorySpecification.builder()
                .id(50)
                .category(tmtCategory)
                .specification(gradeSpec)
                .required(true)
                .displayOrder(1)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(10))
                .thenReturn(List.of(mapping));

        List<CategorySpecificationResponse> results = specificationService.getCategorySpecifications(10, true);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Grade");
        assertThat(results.get(0).getInputType()).isEqualTo(SpecificationInputType.DROPDOWN);
    }

    @Test
    @DisplayName("Update category specification mapping successfully")
    void updateCategorySpecification_Success() {
        when(categoryRepository.findById(10)).thenReturn(Optional.of(tmtCategory));
        CategorySpecification mapping = CategorySpecification.builder()
                .id(50)
                .category(tmtCategory)
                .specification(gradeSpec)
                .required(false)
                .displayOrder(5)
                .active(true)
                .build();

        when(categorySpecificationRepository.findByCategory_CategoryIdAndSpecification_SpecificationId(10, 1))
                .thenReturn(Optional.of(mapping));
        when(categorySpecificationRepository.save(any(CategorySpecification.class))).thenReturn(mapping);

        CategorySpecificationUpdateRequest updateReq = CategorySpecificationUpdateRequest.builder()
                .required(true)
                .displayOrder(1)
                .build();

        CategorySpecificationResponse resp = specificationService.updateCategorySpecification(10, 1, updateReq);

        assertThat(resp).isNotNull();
        verify(categorySpecificationRepository).save(mapping);
    }
}
