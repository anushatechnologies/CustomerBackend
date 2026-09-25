package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.QuotationResponse;
import com.example.project.customer.dto.RfqResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.repository.AdminUserRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.service.FirebaseAuthService;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.service.RfqService;
import com.example.project.customer.service.S3ImageService;
import com.example.project.customer.service.UserService;
import com.example.project.customer.service.WalletService;
import com.example.project.customer.service.location.LocationService;
import com.example.project.customer.service.location.ServiceabilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        FileUploadController.class,
        LocationController.class,
        ServiceabilityController.class,
        InvoiceController.class,
        OrderController.class,
        RfqController.class,
        CreditController.class,
        AuthController.class
})
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@SuppressWarnings("null")
class NewMarketplaceEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3ImageService s3ImageService;

    @MockBean
    private LocationService locationService;

    @MockBean
    private ServiceabilityService serviceabilityService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private UserContextUtil userContextUtil;

    @MockBean
    private RfqService rfqService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private UserService userService;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private AdminUserRepository adminUserRepository;

    // ── 1. Document & File Upload ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/upload - Customer can upload KYC document directly")
    void uploadDirect_Customer_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "kyc_pan.pdf", "application/pdf", "%PDF-sample-content".getBytes()
        );

        ImageUploadResponse uploadRes = ImageUploadResponse.builder()
                .imageKey("kyc/kyc_pan-123.pdf")
                .fileUrl("https://s3.amazonaws.com/kyc/kyc_pan-123.pdf")
                .fileName("kyc_pan.pdf")
                .mimeType("application/pdf")
                .fileSize(18L)
                .build();

        when(s3ImageService.uploadImage(any(), eq("kyc"))).thenReturn(uploadRes);

        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .param("folder", "kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.url").value("https://s3.amazonaws.com/kyc/kyc_pan-123.pdf"))
                .andExpect(jsonPath("$.fileName").value("kyc_pan.pdf"));
    }

    // ── 2. Pincode & Delivery Serviceability ────────────────────────────────

    @Test
    @DisplayName("GET /api/locations/pincode/500081 - Public pincode check succeeds")
    void getPincode_Success() throws Exception {
        com.example.project.customer.dto.PincodeServiceabilityResponse pinResp =
                com.example.project.customer.dto.PincodeServiceabilityResponse.builder()
                        .pincode("500081")
                        .city("Hyderabad")
                        .state("Telangana")
                        .serviceable(true)
                        .estimatedDays(2)
                        .isExpressAvailable(true)
                        .build();

        when(serviceabilityService.checkPincode("500081")).thenReturn(pinResp);

        mockMvc.perform(get("/api/locations/pincode/500081"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pincode").value("500081"))
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.serviceable").value(true));
    }

    @Test
    @DisplayName("GET /api/serviceability/check?pincode=500081 - Public serviceability check succeeds")
    void checkServiceability_Success() throws Exception {
        com.example.project.customer.dto.PincodeServiceabilityResponse pinResp =
                com.example.project.customer.dto.PincodeServiceabilityResponse.builder()
                        .pincode("500081")
                        .city("Hyderabad")
                        .state("Telangana")
                        .serviceable(true)
                        .estimatedDays(2)
                        .isExpressAvailable(true)
                        .build();

        when(serviceabilityService.checkPincode("500081")).thenReturn(pinResp);

        mockMvc.perform(get("/api/serviceability/check").param("pincode", "500081"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceable").value(true))
                .andExpect(jsonPath("$.city").value("Hyderabad"));
    }

    // ── 3. Invoices ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/invoices - Retrieves authenticated customer tax invoices")
    void getInvoices_Success() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(10);

        Order sampleOrder = Order.builder()
                .orderId(101)
                .orderNumber("ORD-2026-101")
                .storeInvoiceNumber("INV-HM-2026-001")
                .totalAmount(BigDecimal.valueOf(15000.00))
                .createdAt(LocalDateTime.now())
                .paymentStatus("PAID")
                .orderStatus("DELIVERED")
                .build();

        when(orderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(10))
                .thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].invoiceNumber").value("INV-HM-2026-001"))
                .andExpect(jsonPath("$.data[0].amount").value(15000.00));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/invoices/101/download-pdf - Downloads invoice PDF")
    void downloadInvoicePdf_Success() throws Exception {
        when(orderService.generateInvoicePdf(101)).thenReturn("%PDF-invoice-dummy".getBytes());

        mockMvc.perform(get("/api/invoices/101/download-pdf"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    org.junit.jupiter.api.Assertions.assertEquals("application/pdf", result.getResponse().getContentType());
                });
    }

    // ── 4. Order Lifecycle additions ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/orders/101/cancel - Supports cancellation via POST")
    void cancelOrder_Post_Success() throws Exception {
        OrderResponse mockOrder = OrderResponse.builder()
                .orderId(101)
                .orderNumber("ORD-101")
                .orderStatus("CANCELLED")
                .build();

        when(orderService.cancelOrder(eq(101), any(), any())).thenReturn(mockOrder);

        mockMvc.perform(post("/api/orders/101/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Changed mind before dispatch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/orders/101/return - Submits dispute/return request")
    void returnOrder_Success() throws Exception {
        com.example.project.customer.dto.OrderDisputeResponse disputeRes =
                com.example.project.customer.dto.OrderDisputeResponse.builder()
                        .success(true)
                        .disputeId("DISP-101-12345")
                        .orderId(101)
                        .status("RETURN_REQUESTED")
                        .reason("Damaged on delivery")
                        .build();

        when(orderService.raiseDispute(eq(101), any())).thenReturn(disputeRes);

        mockMvc.perform(post("/api/orders/101/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Damaged on delivery\", \"description\": \"Rebar bent during transit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RETURN_REQUESTED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/orders/101/mtc - Retrieves Mill Test Certificate")
    void getOrderMtc_Success() throws Exception {
        com.example.project.customer.dto.OrderMtcResponse mtcRes =
                com.example.project.customer.dto.OrderMtcResponse.builder()
                        .orderId(101)
                        .certificateNumber("MTC-ORD-101-QC")
                        .productName("Fe550D TMT Rebar")
                        .grade("IS 1786 Fe550D")
                        .verified(true)
                        .status("VERIFIED")
                        .build();

        when(orderService.getOrderMtc(101)).thenReturn(mtcRes);

        mockMvc.perform(get("/api/orders/101/mtc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificateNumber").value("MTC-ORD-101-QC"));
    }

    // ── 5. RFQ Quote Negotiation ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/rfqs/quotes/5/reject - Rejects vendor quote")
    void rejectQuote_Success() throws Exception {
        QuotationResponse qRes = QuotationResponse.builder()
                .quoteId(5)
                .status("REJECTED")
                .build();

        when(rfqService.rejectQuotation(eq(5), any())).thenReturn(qRes);

        mockMvc.perform(post("/api/rfqs/quotes/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Price too high\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/rfqs/quotes/5/counter - Submits counter offer")
    void counterQuote_Success() throws Exception {
        QuotationResponse qRes = QuotationResponse.builder()
                .quoteId(5)
                .status("COUNTERED")
                .unitPrice(BigDecimal.valueOf(52000.00))
                .build();

        when(rfqService.counterQuotation(eq(5), any())).thenReturn(qRes);

        mockMvc.perform(post("/api/rfqs/quotes/5/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"counterPrice\": 52000.00, \"quantity\": 10, \"notes\": \"Target budget\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COUNTERED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/rfqs/12/close - Closes RFQ")
    void closeRfq_Success() throws Exception {
        RfqResponse rfqRes = RfqResponse.builder()
                .rfqId(12)
                .status("CLOSED")
                .build();

        when(rfqService.closeRfq(eq(12), any())).thenReturn(rfqRes);

        mockMvc.perform(patch("/api/rfqs/12/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Project postponed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    // ── 6. Credit Financing ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/credit/apply - Submits credit application")
    void applyCredit_Success() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(20);

        mockMvc.perform(post("/api/credit/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\": \"Apex Infra Ltd\", \"gstin\": \"36AAACA1234A1Z5\", \"requestedLimit\": 1000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.requestedLimit").value(1000000));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/credit/ledger - Retrieves credit ledger")
    void getCreditLedger_Success() throws Exception {
        when(userContextUtil.getCurrentUserId()).thenReturn(20);

        mockMvc.perform(get("/api/credit/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currency").value("INR"));
    }

    // ── 7. Auth Session ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/logout - Public logout succeeds")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/refresh-token - Refresh token endpoint succeeds")
    void refreshToken_Success() throws Exception {
        mockMvc.perform(post("/api/auth/refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
