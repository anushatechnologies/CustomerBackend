package com.example.project.customer.service;

import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerProductTaxonomyWorkflowTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductTaxonomyService productTaxonomyService;

    private SellerProductServiceImpl sellerProductService;

    private Seller seller;
    private Store store;
    private Category categoryCctv;
    private Subcategory subcategoryCctvCables;
    private Subcategory subcategoryPowerCables;
    private Brand brandFinolex;

    @BeforeEach
    void setUp() {
        sellerProductService = new SellerProductServiceImpl(
                productRepository,
                brandRepository,
                sellerRepository,
                storeRepository,
                productTaxonomyService
        );

        seller = Seller.builder().sellerId(101).name("Seller Alpha").build();
        store = Store.builder().storeId(1).name("Main Store").seller(seller).build();

        categoryCctv = Category.builder().categoryId(50).name("CCTV & Security").build();

        subcategoryCctvCables = Subcategory.builder()
                .subcategoryId(301)
                .name("CCTV Cables")
                .category(categoryCctv)
                .build();

        subcategoryPowerCables = Subcategory.builder()
                .subcategoryId(302)
                .name("Power Cables")
                .category(categoryCctv)
                .build();

        brandFinolex = Brand.builder()
                .brandId(401)
                .name("Finolex")
                .subcategory(subcategoryPowerCables) // Brand linked to Power Cables
                .build();
    }

    @Test
    @DisplayName("Seller creates product: explicit subcategoryId is resolved and stored directly on product")
    void createSellerProduct_ResolvesAndSetsExplicitSubcategory() {
        SellerProductCreateRequest request = SellerProductCreateRequest.builder()
                .title("RG6 Coaxial CCTV Cable 90m")
                .brandId(401)
                .subcategoryId(301)
                .categoryId(50)
                .price(new BigDecimal("1200.00"))
                .unit("coil")
                .build();

        when(brandRepository.findById(401)).thenReturn(Optional.of(brandFinolex));
        when(sellerRepository.findById(101)).thenReturn(Optional.of(seller));
        when(storeRepository.findBySellerSellerId(101)).thenReturn(Optional.of(store));

        when(productTaxonomyService.resolveSubcategoryForProduct(eq(301), eq(50), isNull(), eq(brandFinolex)))
                .thenReturn(subcategoryCctvCables);

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = sellerProductService.createSellerProduct(101, request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product savedProduct = captor.getValue();

        assertThat(savedProduct.getSubcategory()).isEqualTo(subcategoryCctvCables);
        assertThat(response.getSubcategoryId()).isEqualTo(301);
        assertThat(response.getSubcategoryName()).isEqualTo("CCTV Cables");
        assertThat(response.getCategoryId()).isEqualTo(50);
        assertThat(response.getCategoryName()).isEqualTo("CCTV & Security");
    }

    @Test
    @DisplayName("Seller updates product: subcategoryId is updated on the product")
    void updateSellerProduct_UpdatesSubcategory() {
        Product existingProduct = Product.builder()
                .productId(1017)
                .seller(seller)
                .store(store)
                .brand(brandFinolex)
                .subcategory(subcategoryPowerCables)
                .title("Coaxial Cable")
                .build();

        SellerProductUpdateRequest request = SellerProductUpdateRequest.builder()
                .subcategoryId(301)
                .categoryId(50)
                .build();

        when(productRepository.findById(1017)).thenReturn(Optional.of(existingProduct));
        when(productTaxonomyService.resolveSubcategoryForProduct(eq(301), eq(50), eq(existingProduct), eq(brandFinolex)))
                .thenReturn(subcategoryCctvCables);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = sellerProductService.updateSellerProduct(101, 1017, request);

        assertThat(existingProduct.getSubcategory()).isEqualTo(subcategoryCctvCables);
        assertThat(response.getSubcategoryId()).isEqualTo(301);
    }

    @Test
    @DisplayName("Seller updates product: omitted subcategoryId preserves existing product subcategory")
    void updateSellerProduct_PreservesExistingSubcategoryWhenOmitted() {
        Product existingProduct = Product.builder()
                .productId(1018)
                .seller(seller)
                .store(store)
                .brand(brandFinolex)
                .subcategory(subcategoryCctvCables)
                .title("CCTV 3+1 Cable")
                .build();

        SellerProductUpdateRequest request = SellerProductUpdateRequest.builder()
                .price(new BigDecimal("999.00"))
                .build();

        when(productRepository.findById(1018)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = sellerProductService.updateSellerProduct(101, 1018, request);

        assertThat(existingProduct.getSubcategory()).isEqualTo(subcategoryCctvCables);
        assertThat(response.getSubcategoryId()).isEqualTo(301);
    }
}
