package com.example.project.customer;

import com.example.project.customer.dto.CategorySpecificationRequest;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SpecificationRequest;
import com.example.project.customer.dto.SpecificationResponse;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CategorySpecification;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Specification;
import com.example.project.customer.entity.SpecificationInputType;
import com.example.project.customer.entity.SpecificationOption;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.converter.StringMapConverter;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CategorySpecificationRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.SpecificationOptionRepository;
import com.example.project.customer.repository.SpecificationRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.service.ProductServiceImpl;
import com.example.project.customer.service.ProductSpecificationValidator;
import com.example.project.customer.service.S3ImageService;
import com.example.project.customer.service.SellerProductServiceImpl;
import com.example.project.customer.service.SpecificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class ProductSpecificationIntegrationTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategorySpecificationRepository categorySpecificationRepository;
    @Mock
    private SpecificationRepository specificationRepository;
    @Mock
    private SpecificationOptionRepository specificationOptionRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private S3ImageService s3ImageService;

    private ProductSpecificationValidator validator;
    private SpecificationServiceImpl specificationService;
    private ProductServiceImpl productService;
    private SellerProductServiceImpl sellerProductService;

    private Category steelCategory;
    private Subcategory tmtSubcategory;
    private Brand tataBrand;

    private Specification gradeSpec;
    private Specification diameterSpec;
    private Specification yieldStrengthSpec;

    private CategorySpecification csGradeRequired;
    private CategorySpecification csDiameterRequired;
    private CategorySpecification csYieldOptional;

    @BeforeEach
    void setUp() {
        validator = new ProductSpecificationValidator(categorySpecificationRepository, specificationRepository);

        specificationService = new SpecificationServiceImpl(
                specificationRepository,
                specificationOptionRepository,
                categorySpecificationRepository,
                categoryRepository
        );

        productService = new ProductServiceImpl(
                productRepository,
                brandRepository,
                null,
                categoryRepository,
                storeRepository,
                s3ImageService,
                validator
        );

        sellerProductService = new SellerProductServiceImpl(
                productRepository,
                brandRepository,
                sellerRepository,
                storeRepository,
                validator
        );

        steelCategory = Category.builder().categoryId(1).name("Civil & Structural").slug("civil-structural").build();
        tmtSubcategory = Subcategory.builder().subcategoryId(10).category(steelCategory).name("TMT Steel & Rebars").slug("tmt-steel").build();
        tataBrand = Brand.builder().brandId(100).subcategory(tmtSubcategory).name("Tata Tiscon").slug("tata-tiscon").build();

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

        yieldStrengthSpec = Specification.builder()
                .specificationId(3)
                .name("Yield Strength")
                .key("yield_strength")
                .inputType(SpecificationInputType.NUMBER)
                .unit("N/mm²")
                .active(true)
                .build();

        csGradeRequired = CategorySpecification.builder()
                .id(1)
                .category(steelCategory)
                .specification(gradeSpec)
                .required(true)
                .displayOrder(1)
                .active(true)
                .build();

        csDiameterRequired = CategorySpecification.builder()
                .id(2)
                .category(steelCategory)
                .specification(diameterSpec)
                .required(true)
                .displayOrder(2)
                .active(true)
                .build();

        csYieldOptional = CategorySpecification.builder()
                .id(3)
                .category(steelCategory)
                .specification(yieldStrengthSpec)
                .required(false)
                .displayOrder(3)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("End-to-End: Product creation receives specifications, validates, and returns them in response")
    void testProductCreationWithSpecifications() {
        when(brandRepository.findById(100)).thenReturn(Optional.of(tataBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(csGradeRequired, csDiameterRequired, csYieldOptional));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(501);
            return p;
        });

        // The exact payload frontend sends at the end of the creation flow
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Grade", "Fe 550D");
        specs.put("Diameter", "12");
        specs.put("Yield Strength", "550");

        ProductRequest req = ProductRequest.builder()
                .brandId(100)
                .title("Tata Tiscon 550D 12mm TMT Rebars")
                .price(BigDecimal.valueOf(54200))
                .unit("MT")
                .stockQty(500)
                .specifications(specs)
                .build();

        ProductResponse response = productService.create(req);

        // Verification: Data received, validated, and returned in response
        assertNotNull(response);
        assertNotNull(response.getSpecifications());
        assertEquals("Fe 550D", response.getSpecifications().get("Grade"));
        assertEquals("12", response.getSpecifications().get("Diameter"));
        assertEquals("550", response.getSpecifications().get("Yield Strength"));

        // Verify StringMapConverter serializes to JSON for TEXT column
        StringMapConverter converter = new StringMapConverter();
        String dbJson = converter.convertToDatabaseColumn(response.getSpecifications());
        assertTrue(dbJson.contains("\"Grade\":\"Fe 550D\""));
        assertTrue(dbJson.contains("\"Diameter\":\"12\""));
        assertTrue(dbJson.contains("\"Yield Strength\":\"550\""));

        // Verify deserialization back to Map
        Map<String, String> deserialized = converter.convertToEntityAttribute(dbJson);
        assertEquals("Fe 550D", deserialized.get("Grade"));
        assertEquals("12", deserialized.get("Diameter"));
        assertEquals("550", deserialized.get("Yield Strength"));
    }

    @Test
    @DisplayName("End-to-End: Seller product creation receives specifications and validates correctly")
    void testSellerProductCreationWithSpecifications() {
        when(brandRepository.findById(100)).thenReturn(Optional.of(tataBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(csGradeRequired, csDiameterRequired, csYieldOptional));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(601);
            return p;
        });

        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Grade", "Fe 500");
        specs.put("Diameter", "16");

        SellerProductCreateRequest req = SellerProductCreateRequest.builder()
                .brandId(100)
                .title("Tata Tiscon 500 16mm TMT Rebars")
                .price(BigDecimal.valueOf(53000))
                .unit("MT")
                .stockQty(200)
                .specifications(specs)
                .build();

        ProductResponse response = sellerProductService.createSellerProduct(1001, req);

        assertNotNull(response);
        assertEquals("Fe 500", response.getSpecifications().get("Grade"));
        assertEquals("16", response.getSpecifications().get("Diameter"));
    }

    @Test
    @DisplayName("End-to-End: Rejects product when required specification is missing")
    void testProductCreation_MissingRequiredSpecification_Rejected() {
        when(brandRepository.findById(100)).thenReturn(Optional.of(tataBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(csGradeRequired, csDiameterRequired, csYieldOptional));

        // Missing required "Diameter"
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Grade", "Fe 550D");

        ProductRequest req = ProductRequest.builder()
                .brandId(100)
                .title("Tata Tiscon Incomplete Specs")
                .price(BigDecimal.valueOf(54200))
                .unit("MT")
                .specifications(specs)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.create(req));
        assertTrue(ex.getMessage().contains("Specification 'Diameter' is required"));
    }

    @Test
    @DisplayName("End-to-End: Rejects product when dropdown option is invalid")
    void testProductCreation_InvalidDropdownOption_Rejected() {
        when(brandRepository.findById(100)).thenReturn(Optional.of(tataBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(csGradeRequired, csDiameterRequired, csYieldOptional));

        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("Grade", "Invalid Grade XYZ");
        specs.put("Diameter", "12");

        ProductRequest req = ProductRequest.builder()
                .brandId(100)
                .title("Tata Tiscon Invalid Grade")
                .price(BigDecimal.valueOf(54200))
                .unit("MT")
                .specifications(specs)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.create(req));
        assertTrue(ex.getMessage().contains("Invalid value 'Invalid Grade XYZ' for specification 'Grade'"));
    }
}
