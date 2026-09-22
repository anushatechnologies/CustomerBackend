package com.example.project.customer;

import com.example.project.customer.dto.CategorySpecificationRequest;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.dto.SpecificationRequest;
import com.example.project.customer.dto.SpecificationResponse;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CategorySpecification;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // 50-spec category setup
    private Category electricalCategory;
    private Subcategory cableSubcategory;
    private Brand havellsBrand;
    private List<CategorySpecification> electrical50Mappings;
    private List<Specification> electrical50Specs;

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

        // 50 Specifications category
        electricalCategory = Category.builder().categoryId(2).name("Electrical Products").slug("electrical-products").build();
        cableSubcategory = Subcategory.builder().subcategoryId(20).category(electricalCategory).name("Electrical Cable").slug("electrical-cable").build();
        havellsBrand = Brand.builder().brandId(200).subcategory(cableSubcategory).name("Havells").slug("havells").build();

        electrical50Specs = new ArrayList<>();
        electrical50Mappings = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            boolean isReq = (i <= 5); // 5 required, 45 optional
            Specification spec = Specification.builder()
                    .specificationId(100 + i)
                    .name("Spec " + i)
                    .key("spec_" + i)
                    .inputType(SpecificationInputType.TEXT)
                    .active(true)
                    .build();
            electrical50Specs.add(spec);

            CategorySpecification cs = CategorySpecification.builder()
                    .id(200 + i)
                    .category(electricalCategory)
                    .specification(spec)
                    .required(isReq)
                    .displayOrder(i)
                    .active(true)
                    .build();
            electrical50Mappings.add(cs);
        }
    }

    @Test
    @DisplayName("End-to-End: Product creation receives specifications, validates, stores canonical keys, and converts to JSON")
    void testProductCreationWithSpecifications() {
        when(brandRepository.findById(100)).thenReturn(Optional.of(tataBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(1))
                .thenReturn(List.of(csGradeRequired, csDiameterRequired, csYieldOptional));

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(501);
            return p;
        });

        // The exact payload frontend sends at the end of the creation flow (can use display names or keys)
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

        // Verification: Data received, validated, and normalized to canonical keys
        assertNotNull(response);
        assertNotNull(response.getSpecifications());
        assertEquals("Fe 550D", response.getSpecifications().get("grade"));
        assertEquals("12", response.getSpecifications().get("diameter"));
        assertEquals("550", response.getSpecifications().get("yield_strength"));

        // Verify StringMapConverter serializes to JSON for TEXT column
        StringMapConverter converter = new StringMapConverter();
        String dbJson = converter.convertToDatabaseColumn(response.getSpecifications());
        assertTrue(dbJson.contains("\"grade\":\"Fe 550D\""));
        assertTrue(dbJson.contains("\"diameter\":\"12\""));
        assertTrue(dbJson.contains("\"yield_strength\":\"550\""));

        // Verify deserialization back to Map
        Map<String, String> deserialized = converter.convertToEntityAttribute(dbJson);
        assertEquals("Fe 550D", deserialized.get("grade"));
        assertEquals("12", deserialized.get("diameter"));
        assertEquals("550", deserialized.get("yield_strength"));
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
        specs.put("grade", "Fe 500");
        specs.put("diameter", "16");

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
        assertEquals("Fe 500", response.getSpecifications().get("grade"));
        assertEquals("16", response.getSpecifications().get("diameter"));
    }

    @Test
    @DisplayName("Test Case 1: Category has 50 specs, 5 required. Seller selects 5 required only -> SUCCESS (5 stored)")
    void testCase1_Category50Specs_5Required_Success() {
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(701);
            return p;
        });

        // 5 required selected only
        Map<String, String> specs = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            specs.put("spec_" + i, "Value_" + i);
        }

        ProductRequest req = ProductRequest.builder()
                .brandId(200)
                .title("Havells Cable Standard")
                .price(BigDecimal.valueOf(1500))
                .unit("Coil")
                .stockQty(50)
                .specifications(specs)
                .build();

        ProductResponse response = productService.create(req);
        assertNotNull(response);
        assertEquals(5, response.getSpecifications().size());
        for (int i = 1; i <= 5; i++) {
            assertEquals("Value_" + i, response.getSpecifications().get("spec_" + i));
        }
        // Unselected optional specs are NOT stored
        assertNull(response.getSpecifications().get("spec_6"));
    }

    @Test
    @DisplayName("Test Case 2: Category has 50 specs, 5 required. Seller selects 5 required + 3 optional -> SUCCESS (8 stored)")
    void testCase2_Category50Specs_5RequiredPlus3Optional_Success() {
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(702);
            return p;
        });

        // 5 required + 3 optional (spec_6, spec_7, spec_8)
        Map<String, String> specs = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            specs.put("spec_" + i, "Value_" + i);
        }
        specs.put("spec_6", "Voltage 1100V");
        specs.put("spec_7", "Insulation PVC");
        specs.put("spec_8", "Length 90m");

        ProductRequest req = ProductRequest.builder()
                .brandId(200)
                .title("Havells Cable 3-Core 90m")
                .price(BigDecimal.valueOf(2500))
                .unit("Coil")
                .stockQty(30)
                .specifications(specs)
                .build();

        ProductResponse response = productService.create(req);
        assertNotNull(response);
        assertEquals(8, response.getSpecifications().size(), "Product must contain exactly 8 specifications");
        assertEquals("Voltage 1100V", response.getSpecifications().get("spec_6"));
        assertEquals("Insulation PVC", response.getSpecifications().get("spec_7"));
        assertEquals("Length 90m", response.getSpecifications().get("spec_8"));
        assertNull(response.getSpecifications().get("spec_9"));
    }

    @Test
    @DisplayName("Test Case 3: Seller submits only 4 required specifications out of 5 -> FAIL")
    void testCase3_MissingRequiredSpecification_Fails() {
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        // Missing spec_5
        Map<String, String> specs = new LinkedHashMap<>();
        for (int i = 1; i <= 4; i++) {
            specs.put("spec_" + i, "Value_" + i);
        }

        ProductRequest req = ProductRequest.builder()
                .brandId(200)
                .title("Incomplete Cable Product")
                .price(BigDecimal.valueOf(1500))
                .unit("Coil")
                .specifications(specs)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.create(req));
        assertTrue(ex.getMessage().contains("Specification 'Spec 5' is required for this category"));
    }

    @Test
    @DisplayName("Test Case 4: Seller tries to add an unrelated specification -> FAIL backend validation")
    void testCase4_UnrelatedSpecification_FailsValidation() {
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        // 5 required + 1 unrelated spec
        Map<String, String> specs = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            specs.put("spec_" + i, "Value_" + i);
        }
        specs.put("unrelated_specification_xyz", "Some Value");

        ProductRequest req = ProductRequest.builder()
                .brandId(200)
                .title("Unrelated Spec Cable")
                .price(BigDecimal.valueOf(1500))
                .unit("Coil")
                .specifications(specs)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.create(req));
        assertTrue(ex.getMessage().contains("does not exist") || ex.getMessage().contains("does not belong"));
    }

    @Test
    @DisplayName("Test Case 5: Seller adds same specification twice -> FAIL backend validation")
    void testCase5_DuplicateSpecification_FailsValidation() {
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        // Map cannot naturally contain identical keys, but seller might submit key AND display name for same spec
        Map<String, String> specs = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            specs.put("spec_" + i, "Value_" + i);
        }
        specs.put("spec_6", "Voltage 1100V");
        specs.put("Spec 6", "Voltage 1100V Alt"); // Matches spec 106 as well

        ProductRequest req = ProductRequest.builder()
                .brandId(200)
                .title("Duplicate Spec Product")
                .price(BigDecimal.valueOf(1500))
                .unit("Coil")
                .specifications(specs)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.create(req));
        assertTrue(ex.getMessage().contains("Duplicate specification provided"));
    }

    @Test
    @DisplayName("Test Case 6: Product A and Product B in same category use different optional specs -> Both store different subsets")
    void testCase6_DistinctProductsDifferentSubsets_Success() {
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            return p;
        });

        // Product A: 5 required + spec_6 (Voltage), spec_7 (Insulation)
        Map<String, String> specsA = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            specsA.put("spec_" + i, "A_Val_" + i);
        }
        specsA.put("spec_6", "1100V");
        specsA.put("spec_7", "PVC");

        ProductRequest reqA = ProductRequest.builder()
                .brandId(200)
                .title("Product A Cable")
                .price(BigDecimal.valueOf(1000))
                .unit("Coil")
                .specifications(specsA)
                .build();
        ProductResponse resA = productService.create(reqA);

        // Product B: 5 required + spec_8 (Length), spec_9 (Color), spec_10 (Fire Rating)
        Map<String, String> specsB = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            specsB.put("spec_" + i, "B_Val_" + i);
        }
        specsB.put("spec_8", "100m");
        specsB.put("spec_9", "Red");
        specsB.put("spec_10", "FR");

        ProductRequest reqB = ProductRequest.builder()
                .brandId(200)
                .title("Product B Cable")
                .price(BigDecimal.valueOf(1200))
                .unit("Coil")
                .specifications(specsB)
                .build();
        ProductResponse resB = productService.create(reqB);

        assertEquals(7, resA.getSpecifications().size());
        assertTrue(resA.getSpecifications().containsKey("spec_6"));
        assertTrue(resA.getSpecifications().containsKey("spec_7"));
        assertFalse(resA.getSpecifications().containsKey("spec_8"));

        assertEquals(8, resB.getSpecifications().size());
        assertTrue(resB.getSpecifications().containsKey("spec_8"));
        assertTrue(resB.getSpecifications().containsKey("spec_9"));
        assertTrue(resB.getSpecifications().containsKey("spec_10"));
        assertFalse(resB.getSpecifications().containsKey("spec_6"));
    }

    @Test
    @DisplayName("Test Case 7: Edit Product A -> Existing selected specs appear, unselected remain pool, updating persists subset")
    void testCase7_EditProduct_UpdatesSelectedSubset() {
        Map<String, String> existingSpecs = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            existingSpecs.put("spec_" + i, "Original_" + i);
        }
        existingSpecs.put("spec_6", "1100V");

        Product existingProduct = Product.builder()
                .productId(801)
                .title("Original Cable Product")
                .slug("original-cable-product")
                .brand(havellsBrand)
                .price(BigDecimal.valueOf(1000))
                .unit("Coil")
                .specifications(new LinkedHashMap<>(existingSpecs))
                .active(true)
                .build();

        when(productRepository.findById(801)).thenReturn(Optional.of(existingProduct));
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Seller edits: modifies spec_6, adds spec_7, removes nothing
        Map<String, String> updatedSpecs = new LinkedHashMap<>(existingSpecs);
        updatedSpecs.put("spec_6", "3300V"); // Modified
        updatedSpecs.put("spec_7", "XLPE");  // Added from optional pool

        ProductRequest updateReq = ProductRequest.builder()
                .brandId(200)
                .title("Original Cable Product Updated")
                .price(BigDecimal.valueOf(1100))
                .unit("Coil")
                .specifications(updatedSpecs)
                .build();

        ProductResponse response = productService.update(801, updateReq);
        assertNotNull(response);
        assertEquals(7, response.getSpecifications().size());
        assertEquals("3300V", response.getSpecifications().get("spec_6"));
        assertEquals("XLPE", response.getSpecifications().get("spec_7"));
    }

    @Test
    @DisplayName("Test Case 8: Change category during product edit -> Enforces new category pool & required specs")
    void testCase8_CategoryChangeDuringEdit_EnforcesNewCategoryRules() {
        // Product originally in Steel category
        Map<String, String> steelSpecs = new LinkedHashMap<>();
        steelSpecs.put("grade", "Fe 550D");
        steelSpecs.put("diameter", "12");

        Product existingSteelProduct = Product.builder()
                .productId(901)
                .title("Old Steel Rebar Product")
                .slug("old-steel-rebar-product")
                .brand(tataBrand)
                .price(BigDecimal.valueOf(50000))
                .unit("MT")
                .specifications(steelSpecs)
                .active(true)
                .build();

        when(productRepository.findById(901)).thenReturn(Optional.of(existingSteelProduct));
        // Changing to electrical category brand
        when(brandRepository.findById(200)).thenReturn(Optional.of(havellsBrand));
        when(categorySpecificationRepository.findByCategory_CategoryIdAndActiveTrueOrderByDisplayOrderAsc(2))
                .thenReturn(electrical50Mappings);

        // 1. If seller tries to submit old steel specifications for the electrical category -> FAILS
        ProductRequest invalidReq = ProductRequest.builder()
                .brandId(200)
                .title("Moved to Electrical Cable")
                .price(BigDecimal.valueOf(2000))
                .unit("Coil")
                .specifications(steelSpecs)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.update(901, invalidReq));
        assertTrue(ex.getMessage().contains("required") || ex.getMessage().contains("does not belong"));

        // 2. When seller provides valid specifications for new category -> SUCCESS
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> validElectricalSpecs = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            validElectricalSpecs.put("spec_" + i, "NewCategoryVal_" + i);
        }
        ProductRequest validReq = ProductRequest.builder()
                .brandId(200)
                .title("Moved to Electrical Cable Valid")
                .price(BigDecimal.valueOf(2000))
                .unit("Coil")
                .specifications(validElectricalSpecs)
                .build();

        ProductResponse response = productService.update(901, validReq);
        assertNotNull(response);
        assertEquals(5, response.getSpecifications().size());
        assertFalse(response.getSpecifications().containsKey("grade"));
        assertFalse(response.getSpecifications().containsKey("diameter"));
        assertEquals("NewCategoryVal_1", response.getSpecifications().get("spec_1"));
    }
}
