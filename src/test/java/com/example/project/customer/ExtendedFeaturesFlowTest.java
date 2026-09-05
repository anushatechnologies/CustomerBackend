package com.example.project.customer;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.controller.BannerController;
import com.example.project.customer.controller.BlogController;
import com.example.project.customer.controller.CategoryController;
import com.example.project.customer.controller.ChatController;
import com.example.project.customer.controller.NewsController;
import com.example.project.customer.controller.OrderController;
import com.example.project.customer.controller.PurchaseOrderController;
import com.example.project.customer.controller.RentalController;
import com.example.project.customer.controller.SupportTicketController;
import com.example.project.customer.controller.VendorDashboardController;
import com.example.project.customer.controller.WalletController;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.BannerResponse;
import com.example.project.customer.dto.BlogArticleResponse;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.dto.ChatMessageRequest;
import com.example.project.customer.dto.ChatMessageResponse;
import com.example.project.customer.dto.ConversationResponse;
import com.example.project.customer.dto.InvoiceResponse;
import com.example.project.customer.dto.NewsItemResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.PurchaseOrderItemRequest;
import com.example.project.customer.dto.PurchaseOrderRequest;
import com.example.project.customer.dto.PurchaseOrderResponse;
import com.example.project.customer.dto.RentalAvailabilityResponse;
import com.example.project.customer.dto.RentalBookingRequest;
import com.example.project.customer.dto.RentalBookingResponse;
import com.example.project.customer.dto.RentalEquipmentResponse;
import com.example.project.customer.dto.RewardVoucherResponse;
import com.example.project.customer.dto.SupportTicketRequest;
import com.example.project.customer.dto.SupportTicketResponse;
import com.example.project.customer.dto.TicketMessageRequest;
import com.example.project.customer.dto.TicketMessageResponse;
import com.example.project.customer.dto.VendorDashboardResponse;
import com.example.project.customer.dto.VendorPaymentsResponse;
import com.example.project.customer.dto.VendorPerformanceResponse;
import com.example.project.customer.dto.WalletInfoResponse;
import com.example.project.customer.dto.WalletTopupRequest;
import com.example.project.customer.dto.WalletTransactionResponse;
import com.example.project.customer.service.BannerService;
import com.example.project.customer.service.BlogService;
import com.example.project.customer.service.CategoryService;
import com.example.project.customer.service.ChatService;
import com.example.project.customer.service.NewsService;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.service.PurchaseOrderService;
import com.example.project.customer.service.RentalService;
import com.example.project.customer.service.SupportTicketService;
import com.example.project.customer.service.VendorDashboardService;
import com.example.project.customer.service.WalletService;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
public class ExtendedFeaturesFlowTest {

    @Mock private CategoryService categoryService;
    @Mock private BannerService bannerService;
    @Mock private OrderService orderService;
    @Mock private WalletService walletService;
    @Mock private VendorDashboardService vendorDashboardService;
    @Mock private PurchaseOrderService purchaseOrderService;
    @Mock private RentalService rentalService;
    @Mock private ChatService chatService;
    @Mock private BlogService blogService;
    @Mock private NewsService newsService;
    @Mock private SupportTicketService supportTicketService;
    @Mock private UserContextUtil userContextUtil;
    @Mock private SellerContextUtil sellerContextUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(
                new CategoryController(categoryService),
                new BannerController(bannerService),
                new OrderController(orderService, userContextUtil),
                new WalletController(walletService, userContextUtil),
                new VendorDashboardController(vendorDashboardService, sellerContextUtil),
                new PurchaseOrderController(purchaseOrderService, userContextUtil),
                new RentalController(rentalService, userContextUtil),
                new ChatController(chatService, userContextUtil),
                new BlogController(blogService),
                new NewsController(newsService),
                new SupportTicketController(supportTicketService, userContextUtil)
        ).build();
    }

    @Test
    @DisplayName("Section 3.1: Category listing returns standard pagination envelope")
    void testCategoryPaginationEnvelope() throws Exception {
        CategoryResponse cat = CategoryResponse.builder().categoryId(1).name("Civil").build();
        PaginationMeta meta = PaginationMeta.builder().page(1).limit(5).totalCount(1L).totalPages(1).build();
        ApiResponse<List<CategoryResponse>> response = ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .message("Categories fetched successfully")
                .data(List.of(cat))
                .pagination(meta)
                .build();

        when(categoryService.getAll(null, false, 1, 5)).thenReturn(response);

        mockMvc.perform(get("/api/categories")
                        .param("page", "1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination").exists())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.limit").value(5))
                .andExpect(jsonPath("$.pagination.totalCount").value(1))
                .andExpect(jsonPath("$.pagination.totalPages").value(1));
    }

    @Test
    @DisplayName("Section 3.2: Banner JSON serialization standardizes on 'active' and excludes 'isActive'")
    void testBannerBooleanNaming() throws Exception {
        BannerResponse banner = BannerResponse.builder()
                .bannerId(1)
                .title("Festive Offer")
                .imageUrl("https://cdn.example.com/banner.jpg")
                .active(true)
                .build();

        when(bannerService.getAllBanners(null, null)).thenReturn(List.of(banner));

        MvcResult result = mockMvc.perform(get("/api/banners"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);
        JsonNode data = root.get("data");

        assertThat(data).isNotNull();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
        JsonNode firstBanner = data.get(0);
        assertThat(firstBanner.has("active")).isTrue();
        assertThat(firstBanner.has("isActive")).isFalse();
    }

    @Test
    @DisplayName("Section 2.3: B2B Tax Invoice metadata and direct PDF download endpoints")
    void testInvoiceAndPdfDownload() throws Exception {
        InvoiceResponse meta = InvoiceResponse.builder()
                .invoiceNumber("INV-2026-001")
                .sellerGstin("36AAACH2026Q1Z1")
                .pdfUrl("/api/orders/1/invoice/download")
                .build();

        byte[] samplePdf = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

        when(orderService.getOrderInvoice(1)).thenReturn(meta);
        when(orderService.generateInvoicePdf(1)).thenReturn(samplePdf);

        // 1. Check Invoice JSON metadata
        mockMvc.perform(get("/api/orders/1/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-2026-001"))
                .andExpect(jsonPath("$.data.sellerGstin").value("36AAACH2026Q1Z1"))
                .andExpect(jsonPath("$.data.pdfUrl").value("/api/orders/1/invoice/download"));

        // 2. Check PDF download stream
        MvcResult pdfResult = mockMvc.perform(get("/api/orders/1/invoice/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotNull();
        String pdfStart = new String(pdfBytes, 0, Math.min(pdfBytes.length, 20), StandardCharsets.ISO_8859_1);
        assertThat(pdfStart).startsWith("%PDF-1.4");
    }

    @Test
    @DisplayName("Section 4.1: Wallet System - Info, Transactions, Rewards, and Topup")
    void testWalletFlow() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        WalletInfoResponse walletInfo = WalletInfoResponse.builder()
                .walletId(1)
                .userId(101)
                .balance(new BigDecimal("250000.00"))
                .currency("INR")
                .tier("PLATINUM")
                .build();
        when(walletService.getWalletInfo(101)).thenReturn(walletInfo);

        WalletTransactionResponse tx = WalletTransactionResponse.builder()
                .id(1)
                .amount(new BigDecimal("5000.00"))
                .type("CREDIT")
                .balanceAfter(new BigDecimal("255000.00"))
                .build();
        ApiResponse<List<WalletTransactionResponse>> txResp = ApiResponse.<List<WalletTransactionResponse>>builder()
                .success(true)
                .data(List.of(tx))
                .pagination(PaginationMeta.builder().page(1).limit(10).totalCount(1L).totalPages(1).build())
                .build();
        when(walletService.getTransactions(101, 1, 10)).thenReturn(txResp);

        RewardVoucherResponse voucher = RewardVoucherResponse.builder()
                .id(1)
                .code("CEMENT10K")
                .discountValue(new BigDecimal("10000.00"))
                .active(true)
                .build();
        when(walletService.getRewards(101)).thenReturn(List.of(voucher));

        WalletInfoResponse toppedUp = WalletInfoResponse.builder()
                .walletId(1)
                .userId(101)
                .balance(new BigDecimal("255000.00"))
                .currency("INR")
                .tier("PLATINUM")
                .build();
        when(walletService.topup(eq(101), any(WalletTopupRequest.class))).thenReturn(toppedUp);

        // GET /api/wallet/info
        mockMvc.perform(get("/api/wallet/info")
                        .header("X-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(250000.00))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.tier").value("PLATINUM"));

        // GET /api/wallet/transactions
        mockMvc.perform(get("/api/wallet/transactions")
                        .header("X-User-Id", "101")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination").exists());

        // GET /api/wallet/rewards
        mockMvc.perform(get("/api/wallet/rewards")
                        .header("X-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // POST /api/wallet/topup
        WalletTopupRequest topup = WalletTopupRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .description("Test project advance top-up")
                .build();

        mockMvc.perform(post("/api/wallet/topup")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(255000.00));
    }

    @Test
    @DisplayName("Section 4.2: Vendor Dashboard - Overview KPIs, Performance, and Settlements")
    void testVendorDashboardFlow() throws Exception {
        when(sellerContextUtil.getCurrentSellerId()).thenReturn(1001);

        VendorDashboardResponse dash = VendorDashboardResponse.builder()
                .sellerId(1001)
                .totalProducts(24L)
                .totalRevenue(new BigDecimal("4850000.00"))
                .recentActivities(List.of(
                        VendorDashboardResponse.VendorRecentActivity.builder()
                                .id("1")
                                .type("ORDER")
                                .message("New PO received")
                                .build()
                ))
                .build();
        when(vendorDashboardService.getDashboard(1001)).thenReturn(dash);

        VendorPerformanceResponse perf = VendorPerformanceResponse.builder()
                .sellerId(1001)
                .fulfillmentRate(98.40)
                .customerRating(4.85)
                .sellerTier("TIER_1_ENTERPRISE")
                .build();
        when(vendorDashboardService.getPerformance(1001)).thenReturn(perf);

        VendorPaymentsResponse pay = VendorPaymentsResponse.builder()
                .sellerId(1001)
                .totalSettledAmount(new BigDecimal("3500000.00"))
                .payouts(List.of())
                .build();
        when(vendorDashboardService.getPayments(1001)).thenReturn(pay);

        // GET /api/vendor/dashboard
        mockMvc.perform(get("/api/vendor/dashboard")
                        .header("X-Seller-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProducts").value(24))
                .andExpect(jsonPath("$.data.totalRevenue").value(4850000.00))
                .andExpect(jsonPath("$.data.recentActivities").isArray());

        // GET /api/vendor/performance
        mockMvc.perform(get("/api/vendor/performance")
                        .header("X-Seller-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fulfillmentRate").value(98.40))
                .andExpect(jsonPath("$.data.customerRating").value(4.85))
                .andExpect(jsonPath("$.data.sellerTier").value("TIER_1_ENTERPRISE"));

        // GET /api/vendor/payments
        mockMvc.perform(get("/api/vendor/payments")
                        .header("X-Seller-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSettledAmount").value(3500000.00))
                .andExpect(jsonPath("$.data.payouts").isArray());
    }

    @Test
    @DisplayName("Section 4.3: Purchase Orders - Create, List, and Approve")
    void testPurchaseOrdersFlow() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        PurchaseOrderResponse createdPo = PurchaseOrderResponse.builder()
                .poId(501)
                .poNumber("PO-2026-000501")
                .userId(101)
                .vendorId(1001)
                .totalAmount(new BigDecimal("540000.00"))
                .status("PENDING_APPROVAL")
                .build();
        when(purchaseOrderService.createPurchaseOrder(eq(101), any(PurchaseOrderRequest.class))).thenReturn(createdPo);

        ApiResponse<List<PurchaseOrderResponse>> poListResp = ApiResponse.<List<PurchaseOrderResponse>>builder()
                .success(true)
                .data(List.of(createdPo))
                .pagination(PaginationMeta.builder().page(1).limit(10).totalCount(1L).totalPages(1).build())
                .build();
        when(purchaseOrderService.getPurchaseOrders(101, null, 1, 10)).thenReturn(poListResp);

        PurchaseOrderResponse approvedPo = PurchaseOrderResponse.builder()
                .poId(501)
                .poNumber("PO-2026-000501")
                .userId(101)
                .vendorId(1001)
                .totalAmount(new BigDecimal("540000.00"))
                .status("APPROVED")
                .build();
        when(purchaseOrderService.approvePurchaseOrder(eq(501), eq("Chief Procurement Officer"))).thenReturn(approvedPo);

        // Create PO
        PurchaseOrderRequest poReq = PurchaseOrderRequest.builder()
                .vendorId(1001)
                .deliveryDate(LocalDate.now().plusDays(7))
                .billingAddress("Skyline Developers Ltd, Nanakramguda, Hyderabad")
                .shippingAddress("Commercial Tower Site B, HITEC City, Hyderabad")
                .paymentTerms("NET_30")
                .notes("Standard test certificates required.")
                .items(List.of(
                        PurchaseOrderItemRequest.builder()
                                .productId(1)
                                .productTitle("Tata Tiscon 550D TMT Rebar 16mm")
                                .quantity(10)
                                .unit("Metric Ton")
                                .unitPrice(new BigDecimal("54000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/purchase-orders")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(poReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.poId").value(501))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        // List POs
        mockMvc.perform(get("/api/purchase-orders")
                        .header("X-User-Id", "101")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Approve PO
        mockMvc.perform(post("/api/purchase-orders/501/approve")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedBy\": \"Chief Procurement Officer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Section 4.4: Equipment Rentals - Catalog, Availability, and Booking")
    void testRentalsFlow() throws Exception {
        RentalEquipmentResponse eqResp = RentalEquipmentResponse.builder()
                .equipmentId(1)
                .name("Tata Hitachi ZX 210 LC Hydraulic Excavator")
                .category("EXCAVATOR")
                .available(true)
                .build();
        when(rentalService.getEquipment("EXCAVATOR", null, null)).thenReturn(List.of(eqResp));

        RentalAvailabilityResponse avail = RentalAvailabilityResponse.builder()
                .equipmentId(1)
                .equipmentName("Tata Hitachi ZX 210 LC Hydraulic Excavator")
                .isAvailable(true)
                .build();
        when(rentalService.checkAvailability(eq(1), any(), any())).thenReturn(avail);

        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        RentalBookingResponse bookingResp = RentalBookingResponse.builder()
                .bookingId(101)
                .equipmentId(1)
                .status("CONFIRMED")
                .build();
        when(rentalService.bookEquipment(eq(101), any(RentalBookingRequest.class))).thenReturn(bookingResp);

        // List Equipment
        mockMvc.perform(get("/api/rentals")
                        .param("category", "EXCAVATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Check Availability
        mockMvc.perform(get("/api/rentals/1/availability")
                        .param("startDate", LocalDate.now().plusDays(20).toString())
                        .param("endDate", LocalDate.now().plusDays(25).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isAvailable").value(true));

        // Book Equipment
        RentalBookingRequest bookingReq = RentalBookingRequest.builder()
                .equipmentId(1)
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(34))
                .siteAddress("Site #44, Knowledge City, HITEC City, Hyderabad")
                .operatorRequired(true)
                .build();

        mockMvc.perform(post("/api/rentals/book")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(101))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Section 4.5: Real-time Chat - List conversations, get messages, send message")
    void testChatFlow() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(101);

        ConversationResponse conv = ConversationResponse.builder()
                .conversationId(1)
                .buyerId(101)
                .sellerId(1001)
                .title("Tata Tiscon Authorized Distributor")
                .build();
        when(chatService.getConversations(101)).thenReturn(List.of(conv));

        ChatMessageResponse msg = ChatMessageResponse.builder()
                .messageId(201)
                .conversationId(1)
                .senderRole("BUYER")
                .content("Can you provide test reports for heat number H-4920?")
                .build();
        ApiResponse<List<ChatMessageResponse>> msgResp = ApiResponse.<List<ChatMessageResponse>>builder()
                .success(true)
                .data(List.of(msg))
                .build();
        when(chatService.getMessages(1, 1, 20)).thenReturn(msgResp);

        ChatMessageResponse sentMsg = ChatMessageResponse.builder()
                .messageId(202)
                .conversationId(1)
                .senderRole("BUYER")
                .content("Can you provide test reports for heat number H-4920?")
                .build();
        when(chatService.sendMessage(eq(1), eq(101), eq("BUYER"), any(ChatMessageRequest.class))).thenReturn(sentMsg);

        // List conversations
        mockMvc.perform(get("/api/chat/conversations")
                        .header("X-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Get messages
        mockMvc.perform(get("/api/chat/conversations/1/messages")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // Send message
        ChatMessageRequest msgReq = ChatMessageRequest.builder()
                .content("Can you provide test reports for heat number H-4920?")
                .messageType("TEXT")
                .build();

        mockMvc.perform(post("/api/chat/conversations/1/messages")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageId").value(202));
    }

    @Test
    @DisplayName("Section 4.6: Content Modules - Blog, News, and Help & Ticketing")
    void testContentModulesFlow() throws Exception {
        // 1. Blog
        BlogArticleResponse article = BlogArticleResponse.builder()
                .articleId(1)
                .title("How to Verify Mill Test Certificates (MTC) for Fe550D Rebars")
                .slug("verify-mtc-fe550d-rebars")
                .category("TECHNICAL_GUIDE")
                .build();
        ApiResponse<List<BlogArticleResponse>> blogResp = ApiResponse.<List<BlogArticleResponse>>builder()
                .success(true)
                .data(List.of(article))
                .build();
        when(blogService.getArticles(null, null, 1, 5)).thenReturn(blogResp);
        when(blogService.getArticleBySlugOrId("verify-mtc-fe550d-rebars")).thenReturn(article);

        mockMvc.perform(get("/api/blog/articles")
                        .param("page", "1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/blog/articles/verify-mtc-fe550d-rebars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").exists());

        // 2. News
        NewsItemResponse news = NewsItemResponse.builder()
                .newsId(1)
                .title("MoRTH Mandates Ultra-High Durability Cement Grades for Highway Bridges")
                .build();
        ApiResponse<List<NewsItemResponse>> newsResp = ApiResponse.<List<NewsItemResponse>>builder()
                .success(true)
                .data(List.of(news))
                .build();
        when(newsService.getNews(null, 1, 10)).thenReturn(newsResp);

        mockMvc.perform(get("/api/news")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 3. Help & Ticketing
        when(userContextUtil.getCurrentUserId()).thenReturn(101);
        SupportTicketResponse tktResp = SupportTicketResponse.builder()
                .ticketId(1001)
                .ticketNumber("TKT-2026-001")
                .subject("Input Tax Credit Split Breakdown for Invoice #INV-2026-001")
                .status("OPEN")
                .build();
        when(supportTicketService.createTicket(eq(101), any(SupportTicketRequest.class))).thenReturn(tktResp);

        TicketMessageResponse msgResp = TicketMessageResponse.builder()
                .messageId(501)
                .ticketId(1001)
                .senderRole("USER")
                .content("Acknowledged. Our finance team will forward the filing acknowledgement.")
                .build();
        when(supportTicketService.addMessage(eq(1001), eq(101), eq("USER"), eq("Customer"), any(TicketMessageRequest.class))).thenReturn(msgResp);

        SupportTicketRequest tktReq = SupportTicketRequest.builder()
                .subject("Input Tax Credit Split Breakdown for Invoice #INV-2026-001")
                .category("PAYMENT")
                .priority("MEDIUM")
                .orderId(1)
                .message("Need formal GSTR-1 acknowledgement number for monthly tax audit.")
                .build();

        mockMvc.perform(post("/api/help/tickets")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tktReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticketId").value(1001));

        // Reply to ticket
        TicketMessageRequest replyReq = TicketMessageRequest.builder()
                .content("Acknowledged. Our finance team will forward the filing acknowledgement.")
                .build();

        mockMvc.perform(post("/api/help/tickets/1001/messages")
                        .header("X-User-Id", "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}
