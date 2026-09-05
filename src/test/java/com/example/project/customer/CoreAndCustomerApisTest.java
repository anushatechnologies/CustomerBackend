package com.example.project.customer;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.controller.CartController;
import com.example.project.customer.controller.CategoryController;
import com.example.project.customer.controller.CheckoutController;
import com.example.project.customer.controller.ProductController;
import com.example.project.customer.controller.RfqController;
import com.example.project.customer.controller.SellerDiscountController;
import com.example.project.customer.controller.SubcategoryController;
import com.example.project.customer.controller.UserProfileController;
import com.example.project.customer.controller.WishlistController;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CategoryRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.CouponRequest;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.dto.ProductRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.QuotationRequest;
import com.example.project.customer.dto.QuotationResponse;
import com.example.project.customer.dto.RfqQuestionRequest;
import com.example.project.customer.dto.RfqQuestionResponse;
import com.example.project.customer.dto.RfqRequest;
import com.example.project.customer.dto.RfqResponse;
import com.example.project.customer.dto.SellerDiscountRequest;
import com.example.project.customer.dto.SellerDiscountResponse;
import com.example.project.customer.dto.StockQuantityUpdateRequest;
import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.dto.UserProfileRequest;
import com.example.project.customer.dto.UserProfileResponse;
import com.example.project.customer.dto.WishlistResponse;
import com.example.project.customer.entity.DiscountStatus;
import com.example.project.customer.entity.DiscountType;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CategoryService;
import com.example.project.customer.service.CheckoutService;
import com.example.project.customer.service.ProductService;
import com.example.project.customer.service.RfqService;
import com.example.project.customer.service.SellerDiscountService;
import com.example.project.customer.service.SubcategoryService;
import com.example.project.customer.service.UserProfileService;
import com.example.project.customer.service.WishlistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
public class CoreAndCustomerApisTest {

    @Mock private CartService cartService;
    @Mock private CheckoutService checkoutService;
    @Mock private UserProfileService userProfileService;
    @Mock private WishlistService wishlistService;
    @Mock private RfqService rfqService;
    @Mock private SellerDiscountService sellerDiscountService;
    @Mock private ProductService productService;
    @Mock private CategoryService categoryService;
    @Mock private SubcategoryService subcategoryService;
    @Mock private UserContextUtil userContextUtil;
    @Mock private SellerContextUtil sellerContextUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(
                new CartController(cartService, userContextUtil),
                new CheckoutController(checkoutService, userContextUtil),
                new UserProfileController(userProfileService, userContextUtil),
                new WishlistController(wishlistService, userContextUtil),
                new RfqController(rfqService, userContextUtil),
                new SellerDiscountController(sellerDiscountService, sellerContextUtil),
                new ProductController(productService),
                new CategoryController(categoryService),
                new SubcategoryController(subcategoryService)
        ).build();
    }

    // ==========================================
    // 1. Cart APIs (/api/cart)
    // ==========================================

    @Test
    @DisplayName("Cart API: GET /api/cart returns user's active cart")
    void testGetCart() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        CartResponse mockCart = CartResponse.builder()
                .cartId(1)
                .subtotal(BigDecimal.valueOf(150000.0))
                .build();
        when(cartService.getCart(101)).thenReturn(mockCart);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cartId").value(1))
                .andExpect(jsonPath("$.message").value("Cart retrieved successfully"));
    }

    @Test
    @DisplayName("Cart API: POST /api/cart/items adds or updates item")
    void testAddCartItem() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        CartItemRequest req = CartItemRequest.builder().productId(10).quantity(5).build();
        CartResponse mockCart = CartResponse.builder().cartId(1).grandTotal(BigDecimal.valueOf(25000.0)).build();
        when(cartService.addItem(eq(101), any(CartItemRequest.class))).thenReturn(mockCart);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cartId").value(1));
    }

    @Test
    @DisplayName("Cart API: DELETE /api/cart/items/{productId} removes item")
    void testRemoveCartItem() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        CartResponse mockCart = CartResponse.builder().cartId(1).grandTotal(BigDecimal.ZERO).build();
        when(cartService.removeItem(101, 10)).thenReturn(mockCart);

        mockMvc.perform(delete("/api/cart/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from cart successfully"));
    }

    @Test
    @DisplayName("Cart API: DELETE /api/cart clears the entire cart")
    void testClearCart() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));

        verify(cartService).clearCart(101);
    }

    @Test
    @DisplayName("Cart API: POST /api/cart/coupon applies coupon successfully")
    void testApplyCoupon() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        CouponRequest req = CouponRequest.builder().code("BUILD2026").build();
        CouponResponse mockCoupon = CouponResponse.builder()
                .couponCode("BUILD2026")
                .discountAmount(BigDecimal.valueOf(5000.0))
                .newGrandTotal(BigDecimal.valueOf(95000.0))
                .build();
        when(cartService.applyCoupon(101, "BUILD2026")).thenReturn(mockCoupon);

        mockMvc.perform(post("/api/cart/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponCode").value("BUILD2026"));
    }

    // ==========================================
    // 2. Checkout APIs (/api/checkout)
    // ==========================================

    @Test
    @DisplayName("Checkout API: POST /api/checkout/preview calculates tax, freight, crane, and totals")
    void testPreviewCheckout() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        CheckoutPreviewRequest req = CheckoutPreviewRequest.builder()
                .addressId(5)
                .deliverySlot("MORNING_08_12")
                .requiresCraneUnloading(true)
                .build();

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(BigDecimal.valueOf(100000.0))
                .totalGst(BigDecimal.valueOf(18000.0))
                .freightCharge(BigDecimal.valueOf(4500.0))
                .craneUnloadingCharge(BigDecimal.valueOf(2500.0))
                .grandTotal(BigDecimal.valueOf(125000.0))
                .build();

        when(checkoutService.previewCheckout(eq(101), any(CheckoutPreviewRequest.class))).thenReturn(preview);

        mockMvc.perform(post("/api/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grandTotal").value(125000.0))
                .andExpect(jsonPath("$.data.totalGst").value(18000.0));
    }

    // ==========================================
    // 3. User Profile APIs (/api/user)
    // ==========================================

    @Test
    @DisplayName("User Profile API: GET /api/user/profile retrieves profile")
    void testGetUserProfile() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(101)
                .fullName("Site Project Manager")
                .email("infra@enterprise.com")
                .build();
        when(userProfileService.getProfile(101)).thenReturn(profile);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Site Project Manager"));
    }

    @Test
    @DisplayName("User Profile API: PUT /api/user/profile updates profile")
    void testUpdateUserProfile() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        UserProfileRequest req = UserProfileRequest.builder()
                .fullName("Chief Procurement Engineer")
                .companyName("Skyline Infra Corp Ltd")
                .build();
        UserProfileResponse updated = UserProfileResponse.builder()
                .id(101)
                .fullName("Chief Procurement Engineer")
                .build();
        when(userProfileService.updateProfile(eq(101), any(UserProfileRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Chief Procurement Engineer"));
    }

    // ==========================================
    // 4. Wishlist APIs (/api/wishlist)
    // ==========================================

    @Test
    @DisplayName("Wishlist API: GET, POST, DELETE workflows")
    void testWishlistEndpoints() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        WishlistResponse item = WishlistResponse.builder()
                .id(1)
                .productId(20)
                .build();

        when(wishlistService.getWishlist(101)).thenReturn(List.of(item));
        when(wishlistService.addToWishlist(101, 20)).thenReturn(item);

        // GET
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value(20));

        // POST
        mockMvc.perform(post("/api/wishlist/20"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(20));

        // DELETE
        mockMvc.perform(delete("/api/wishlist/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product removed from wishlist successfully"));
    }

    // ==========================================
    // 5. RFQ APIs (/api/rfqs)
    // ==========================================

    @Test
    @DisplayName("RFQ API: Create, list, details, quotations, and questions")
    void testRfqEndpoints() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        RfqRequest rfqReq = RfqRequest.builder()
                .title("20 MT TMT Rebar Fe550D 16mm")
                .category("Civil & Structural")
                .productMaterial("TMT Rebars")
                .quantity(20)
                .unit("MT")
                .deliveryLocation("Gachibowli Site, Hyderabad")
                .build();

        RfqResponse rfqResp = RfqResponse.builder()
                .rfqId(1)
                .title("20 MT TMT Rebar Fe550D 16mm")
                .status("OPEN")
                .quantity(20)
                .unit("MT")
                .build();

        when(rfqService.createRfq(eq(101), any(RfqRequest.class))).thenReturn(rfqResp);
        when(rfqService.getRfqs(101, null, 1, 20)).thenReturn(ApiResponse.ok("RFQs retrieved", List.of(rfqResp)));
        when(rfqService.getRfqById(1)).thenReturn(rfqResp);

        // POST RFQ
        mockMvc.perform(post("/api/rfqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rfqReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rfqId").value(1));

        // GET RFQs
        mockMvc.perform(get("/api/rfqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("20 MT TMT Rebar Fe550D 16mm"));

        // GET RFQ by ID
        mockMvc.perform(get("/api/rfqs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rfqId").value(1));

        // Quotations list
        QuotationResponse quote = QuotationResponse.builder()
                .quoteId(10)
                .rfqId(1)
                .vendorName("Patancheru Steel Distributors")
                .unitPrice(BigDecimal.valueOf(53000.0))
                .totalAmount(BigDecimal.valueOf(1060000.0))
                .build();
        when(rfqService.getRfqQuotations(1)).thenReturn(List.of(quote));

        mockMvc.perform(get("/api/rfqs/1/quotations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].quoteId").value(10));

        // Accept quotation
        when(rfqService.acceptQuotation(10)).thenReturn(Map.of("orderId", 501, "status", "CONFIRMED"));
        mockMvc.perform(post("/api/rfqs/quotes/10/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(501));

        // RFQ clarification question
        RfqQuestionRequest qReq = RfqQuestionRequest.builder().question("Is mill test certificate provided?").build();
        RfqQuestionResponse qResp = RfqQuestionResponse.builder().questionId(1).question("Is mill test certificate provided?").build();
        when(rfqService.addRfqQuestion(eq(1), any(RfqQuestionRequest.class))).thenReturn(qResp);

        mockMvc.perform(post("/api/rfqs/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionId").value(1));
    }

    // ==========================================
    // 6. Seller Discount APIs (/api/seller/discounts, /api/admin/discounts)
    // ==========================================

    @Test
    @DisplayName("Seller Discount API: Seller CRUD, Admin approval, Customer listing")
    void testSellerDiscountEndpoints() throws Exception {
        when(sellerContextUtil.getCurrentSellerId()).thenReturn(1001);

        SellerDiscountRequest discReq = new SellerDiscountRequest(
                "MONSOON10",
                DiscountType.PERCENTAGE,
                BigDecimal.valueOf(10.0),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                BigDecimal.valueOf(50000.0),
                BigDecimal.valueOf(15000.0),
                "10% Monsoon Discount",
                true
        );

        SellerDiscountResponse discResp = SellerDiscountResponse.builder()
                .discountId(1)
                .sellerId(1001)
                .code("MONSOON10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10.0))
                .status(DiscountStatus.PENDING)
                .build();

        when(sellerDiscountService.create(eq(1001), any(SellerDiscountRequest.class))).thenReturn(discResp);
        when(sellerDiscountService.getBySeller(1001)).thenReturn(List.of(discResp));
        when(sellerDiscountService.getById(1001, 1)).thenReturn(discResp);

        // POST seller discount
        mockMvc.perform(post("/api/seller/discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(discReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("MONSOON10"));

        // GET seller discounts
        mockMvc.perform(get("/api/seller/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("MONSOON10"));

        // GET seller discount by ID
        mockMvc.perform(get("/api/seller/discounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discountId").value(1));

        // Submit for review
        SellerDiscountResponse submitted = SellerDiscountResponse.builder().discountId(1).status(DiscountStatus.PENDING).build();
        when(sellerDiscountService.submitForReview(1001, 1)).thenReturn(submitted);
        mockMvc.perform(patch("/api/seller/discounts/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // Admin approve
        SellerDiscountResponse approved = SellerDiscountResponse.builder().discountId(1).status(DiscountStatus.APPROVED).build();
        when(sellerDiscountService.approve(1)).thenReturn(approved);
        mockMvc.perform(patch("/api/admin/discounts/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Customer available discounts
        when(sellerDiscountService.getApplicableForCustomer()).thenReturn(List.of(approved));
        mockMvc.perform(get("/api/customer/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==========================================
    // 7. Product APIs (/api/products)
    // ==========================================

    @Test
    @DisplayName("Product API: CRUD, stock update, activation toggle, paginated list")
    void testProductEndpoints() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .productId(1)
                .title("Tata Tiscon Fe550D 16mm")
                .slug("tata-tiscon-fe550d-16mm")
                .price(BigDecimal.valueOf(54000.0))
                .stockQty(150)
                .unit("MT")
                .active(true)
                .build();

        ProductRequest prodReq = ProductRequest.builder()
                .title("Tata Tiscon Fe550D 16mm")
                .price(BigDecimal.valueOf(54000.0))
                .stockQty(150)
                .unit("MT")
                .build();

        when(productService.create(any(ProductRequest.class))).thenReturn(prod);
        when(productService.getById(1)).thenReturn(prod);
        when(productService.getAll(any(), any(), any(), any(), any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(ApiResponse.ok("Products retrieved successfully", List.of(prod)));

        // Create product
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Tata Tiscon Fe550D 16mm"));

        // Get by ID
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(1));

        // Get all paginated
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Tata Tiscon Fe550D 16mm"));

        // Update stock
        StockQuantityUpdateRequest stockReq = new StockQuantityUpdateRequest();
        stockReq.setStockQty(200);
        ProductResponse updatedStock = ProductResponse.builder().productId(1).stockQty(200).build();
        when(productService.updateStockQuantity(eq(1), any(StockQuantityUpdateRequest.class))).thenReturn(updatedStock);

        mockMvc.perform(patch("/api/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQty").value(200));

        // Activate / Deactivate
        when(productService.activate(1)).thenReturn(prod);
        mockMvc.perform(patch("/api/products/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product activated successfully"));

        // Delete product
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));
        verify(productService).delete(1);
    }

    // ==========================================
    // 8. Category & Subcategory APIs
    // ==========================================

    @Test
    @DisplayName("Category & Subcategory APIs: CRUD endpoints")
    void testCategoryAndSubcategoryEndpoints() throws Exception {
        CategoryRequest catReq = CategoryRequest.builder().name("Civil & Structural").build();
        CategoryResponse catResp = CategoryResponse.builder().categoryId(1).name("Civil & Structural").build();

        when(categoryService.create(any(CategoryRequest.class))).thenReturn(catResp);
        when(categoryService.getById(1)).thenReturn(catResp);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Civil & Structural"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(1));

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
        verify(categoryService).delete(1);

        // Subcategory
        SubcategoryRequest subReq = SubcategoryRequest.builder().categoryId(1).name("TMT Steel").build();
        SubcategoryResponse subResp = SubcategoryResponse.builder().subcategoryId(1).name("TMT Steel").build();

        when(subcategoryService.create(any(SubcategoryRequest.class))).thenReturn(subResp);
        when(subcategoryService.getById(1)).thenReturn(subResp);
        when(subcategoryService.getAll(null, null)).thenReturn(List.of(subResp));

        mockMvc.perform(post("/api/subcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("TMT Steel"));

        mockMvc.perform(get("/api/subcategories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("TMT Steel"));

        mockMvc.perform(get("/api/subcategories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subcategoryId").value(1));

        mockMvc.perform(delete("/api/subcategories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subcategory deleted successfully"));
        verify(subcategoryService).delete(1);
    }
}
