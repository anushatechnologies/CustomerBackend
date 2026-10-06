package com.example.project.customer;

import com.example.project.customer.controller.AdminHotDealController;
import com.example.project.customer.controller.HomeHotDealController;
import com.example.project.customer.dto.HomeHotDealsResponse;
import com.example.project.customer.dto.HotDealReorderItem;
import com.example.project.customer.dto.HotDealReorderRequest;
import com.example.project.customer.dto.HotDealRequest;
import com.example.project.customer.dto.HotDealResponse;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.HotDeal;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.HotDealRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.service.HotDealService;
import com.example.project.customer.service.HotDealServiceImpl;
import com.example.project.customer.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HotDealFeatureTest {

    @Mock
    private HotDealRepository hotDealRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private HotDealServiceImpl hotDealService;

    @Mock
    private HotDealService mockHotDealService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Product mockProduct;
    private ProductResponse mockProductResponse;
    private HotDeal mockHotDeal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminHotDealController(mockHotDealService),
                new HomeHotDealController(mockHotDealService)
        ).build();

        mockProduct = Product.builder()
                .productId(101)
                .title("Ultratech Super Cement 50kg")
                .slug("ultratech-super-cement-50kg")
                .sku("CEMT-UT-50KG")
                .price(BigDecimal.valueOf(420.0))
                .sellingPrice(BigDecimal.valueOf(390.0))
                .mrp(BigDecimal.valueOf(450.0))
                .unit("Bag")
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        mockProductResponse = ProductResponse.builder()
                .productId(101)
                .title("Ultratech Super Cement 50kg")
                .price(BigDecimal.valueOf(420.0))
                .sellingPrice(BigDecimal.valueOf(390.0))
                .mrp(BigDecimal.valueOf(450.0))
                .unit("Bag")
                .active(true)
                .displayOrder(1)
                .build();

        mockHotDeal = HotDeal.builder()
                .id(1L)
                .product(mockProduct)
                .displayOrder(1)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================
    // SERVICE UNIT TESTS
    // =========================================================

    @Test
    @DisplayName("Service: addHotDeal successfully adds an existing product")
    void testAddHotDeal_Success() {
        HotDealRequest request = HotDealRequest.builder()
                .productId(101)
                .displayOrder(1)
                .active(true)
                .build();

        when(productRepository.findById(101)).thenReturn(Optional.of(mockProduct));
        when(hotDealRepository.existsByProduct_ProductId(101)).thenReturn(false);
        when(hotDealRepository.save(any(HotDeal.class))).thenReturn(mockHotDeal);
        when(productService.mapToResponse(mockProduct)).thenReturn(mockProductResponse);

        HotDealResponse response = hotDealService.addHotDeal(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductId()).isEqualTo(101);
        assertThat(response.isActive()).isTrue();
        verify(hotDealRepository).save(any(HotDeal.class));
    }

    @Test
    @DisplayName("Service: addHotDeal throws ResourceNotFoundException if product does not exist")
    void testAddHotDeal_ProductNotFound() {
        HotDealRequest request = HotDealRequest.builder().productId(999).build();
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotDealService.addHotDeal(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 999");

        verify(hotDealRepository, never()).save(any(HotDeal.class));
    }

    @Test
    @DisplayName("Service: addHotDeal throws ResourceConflictException on duplicate product")
    void testAddHotDeal_DuplicateProduct() {
        HotDealRequest request = HotDealRequest.builder().productId(101).build();
        when(productRepository.findById(101)).thenReturn(Optional.of(mockProduct));
        when(hotDealRepository.existsByProduct_ProductId(101)).thenReturn(true);

        assertThatThrownBy(() -> hotDealService.addHotDeal(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists in Hot Deals");

        verify(hotDealRepository, never()).save(any(HotDeal.class));
    }

    @Test
    @DisplayName("Service: deleteHotDeal removes HotDeal but does NOT delete Product entity")
    void testDeleteHotDeal_DoesNotDeleteProduct() {
        when(hotDealRepository.findById(1L)).thenReturn(Optional.of(mockHotDeal));

        hotDealService.deleteHotDeal(1L);

        verify(hotDealRepository).delete(mockHotDeal);
        verify(productRepository, never()).delete(any(Product.class));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Service: setStatus enables or disables Hot Deal")
    void testSetStatus() {
        when(hotDealRepository.findById(1L)).thenReturn(Optional.of(mockHotDeal));
        when(hotDealRepository.save(any(HotDeal.class))).thenReturn(mockHotDeal);
        when(productService.mapToResponse(mockProduct)).thenReturn(mockProductResponse);

        HotDealResponse response = hotDealService.setStatus(1L, false);

        assertThat(response).isNotNull();
        verify(hotDealRepository).save(mockHotDeal);
    }

    @Test
    @DisplayName("Service: reorderHotDeals updates displayOrder in batch")
    void testReorderHotDeals() {
        HotDealReorderRequest request = HotDealReorderRequest.builder()
                .items(List.of(HotDealReorderItem.builder().id(1L).displayOrder(5).build()))
                .build();

        when(hotDealRepository.findAll()).thenReturn(List.of(mockHotDeal));
        when(hotDealRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(mockHotDeal));
        when(productService.mapToResponse(mockProduct)).thenReturn(mockProductResponse);

        List<HotDealResponse> result = hotDealService.reorderHotDeals(request);

        assertThat(result).hasSize(1);
        assertThat(mockHotDeal.getDisplayOrder()).isEqualTo(5);
        verify(hotDealRepository).saveAll(any());
    }

    @Test
    @DisplayName("Service: getActiveHotDealsForHome returns only active, approved products")
    void testGetActiveHotDealsForHome() {
        when(hotDealRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(mockHotDeal));
        when(productService.mapToResponse(mockProduct)).thenReturn(mockProductResponse);

        HomeHotDealsResponse response = hotDealService.getActiveHotDealsForHome();

        assertThat(response).isNotNull();
        assertThat(response.getSection()).isEqualTo("Hot Deals");
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getTitle()).isEqualTo("Ultratech Super Cement 50kg");
        assertThat(response.getProducts().get(0).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Service: getActiveHotDealsForHome handles empty state gracefully")
    void testGetActiveHotDealsForHome_EmptyState() {
        when(hotDealRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());

        HomeHotDealsResponse response = hotDealService.getActiveHotDealsForHome();

        assertThat(response).isNotNull();
        assertThat(response.getSection()).isEqualTo("Hot Deals");
        assertThat(response.getProducts()).isEmpty();
        assertThat(response.getTotal()).isEqualTo(0);
    }

    // =========================================================
    // CONTROLLER & API TESTS (MockMvc)
    // =========================================================

    @Test
    @DisplayName("Admin API: POST /api/admin/hot-deals adds product to Hot Deals")
    void testAdminCreateHotDeal() throws Exception {
        HotDealRequest req = HotDealRequest.builder().productId(101).displayOrder(1).active(true).build();
        HotDealResponse resp = HotDealResponse.builder()
                .id(1L)
                .productId(101)
                .displayOrder(1)
                .active(true)
                .product(mockProductResponse)
                .build();

        when(mockHotDealService.addHotDeal(any(HotDealRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/admin/hot-deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(101))
                .andExpect(jsonPath("$.message").value("Product added to Hot Deals successfully"));
    }

    @Test
    @DisplayName("Admin API: GET /api/admin/hot-deals lists all Hot Deals")
    void testAdminListHotDeals() throws Exception {
        HotDealResponse resp = HotDealResponse.builder()
                .id(1L)
                .productId(101)
                .displayOrder(1)
                .active(true)
                .product(mockProductResponse)
                .build();

        when(mockHotDealService.getAdminHotDeals()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/admin/hot-deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].productId").value(101));
    }

    @Test
    @DisplayName("Admin API: PATCH /api/admin/hot-deals/{id}/status toggles active status")
    void testAdminToggleStatus() throws Exception {
        HotDealResponse resp = HotDealResponse.builder()
                .id(1L)
                .productId(101)
                .active(false)
                .build();

        when(mockHotDealService.setStatus(1L, false)).thenReturn(resp);

        mockMvc.perform(patch("/api/admin/hot-deals/1/status")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @DisplayName("Admin API: DELETE /api/admin/hot-deals/{id} deletes Hot Deal")
    void testAdminDeleteHotDeal() throws Exception {
        mockMvc.perform(delete("/api/admin/hot-deals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hot Deal removed successfully"));

        verify(mockHotDealService).deleteHotDeal(1L);
    }

    @Test
    @DisplayName("User API: GET /api/home/hot-deals returns curated Hot Deals for Home Screen")
    void testUserGetHomeHotDeals() throws Exception {
        HomeHotDealsResponse homeResp = HomeHotDealsResponse.builder()
                .section("Hot Deals")
                .products(List.of(mockProductResponse))
                .total(1)
                .build();

        when(mockHotDealService.getActiveHotDealsForHome()).thenReturn(homeResp);

        mockMvc.perform(get("/api/home/hot-deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.section").value("Hot Deals"))
                .andExpect(jsonPath("$.data.products[0].id").value(101))
                .andExpect(jsonPath("$.data.products[0].name").value("Ultratech Super Cement 50kg"))
                .andExpect(jsonPath("$.data.products[0].price").value(420.0))
                .andExpect(jsonPath("$.data.products[0].displayOrder").value(1));
    }

    @Test
    @DisplayName("User API: GET /api/hot-deals alias returns same Home Screen Hot Deals")
    void testUserGetHotDealsAlias() throws Exception {
        HomeHotDealsResponse homeResp = HomeHotDealsResponse.builder()
                .section("Hot Deals")
                .products(List.of(mockProductResponse))
                .total(1)
                .build();

        when(mockHotDealService.getActiveHotDealsForHome()).thenReturn(homeResp);

        mockMvc.perform(get("/api/hot-deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.section").value("Hot Deals"))
                .andExpect(jsonPath("$.data.products[0].id").value(101));
    }
}
