package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.BulkPriceAdjustmentRequest;
import com.example.project.customer.dto.InventoryAdjustmentRequest;
import com.example.project.customer.dto.SellerDocumentItemResponse;
import com.example.project.customer.dto.SellerEnquiryItemResponse;
import com.example.project.customer.dto.SellerQuotationCreateRequest;
import com.example.project.customer.dto.SellerQuotationItemDto;
import com.example.project.customer.dto.SellerQuotationRecordResponse;
import com.example.project.customer.dto.WarehouseRequest;
import com.example.project.customer.entity.Warehouse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.SellerDocumentVaultService;
import com.example.project.customer.service.SellerPricingService;
import com.example.project.customer.service.SellerQuotationManagementService;
import com.example.project.customer.service.SellerWarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        SellerWarehouseController.class,
        SellerInventoryController.class,
        SellerPricingController.class,
        SellerDocumentController.class,
        SellerQuotationController.class
})
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SellerContextUtil.class})
class SellerWarehouseAndInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SellerWarehouseService warehouseService;

    @MockBean
    private SellerPricingService sellerPricingService;

    @MockBean
    private SellerDocumentVaultService documentVaultService;

    @MockBean
    private SellerQuotationManagementService quotationService;

    @Test
    @DisplayName("GET & POST /api/seller/warehouses - Warehouse Operations")
    void warehouseEndpoints_Success() throws Exception {
        Warehouse wh = Warehouse.builder()
                .warehouseId(1)
                .name("Bhiwandi Central Logistics Yard")
                .city("Bhiwandi")
                .isDefault(true)
                .build();

        when(warehouseService.getWarehouses(any())).thenReturn(List.of(wh));

        mockMvc.perform(get("/api/seller/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("wh_1"))
                .andExpect(jsonPath("$.data[0].name").value("Bhiwandi Central Logistics Yard"));

        WarehouseRequest req = WarehouseRequest.builder()
                .name("Chakan Industrial Depot")
                .city("Pune")
                .build();

        when(warehouseService.createWarehouse(any(), any(WarehouseRequest.class)))
                .thenReturn(Warehouse.builder().warehouseId(2).name("Chakan Industrial Depot").city("Pune").build());

        mockMvc.perform(post("/api/seller/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("wh_2"));
    }

    @Test
    @DisplayName("POST /api/seller/inventory/adjust - Stock Adjustment")
    void inventoryAdjust_Success() throws Exception {
        InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                .productId("sp_101")
                .warehouseId("wh_1")
                .adjustmentType("add")
                .quantity(25)
                .reason("New batch")
                .build();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productId", "sp_101");
        map.put("newStock", 110);

        when(warehouseService.adjustInventory(any(), any(InventoryAdjustmentRequest.class)))
                .thenReturn(map);

        mockMvc.perform(post("/api/seller/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newStock").value(110));
    }

    @Test
    @DisplayName("POST /api/seller/pricing/bulk-adjust - Bulk Price Adjustments")
    void bulkPricing_Success() throws Exception {
        BulkPriceAdjustmentRequest req = BulkPriceAdjustmentRequest.builder()
                .categoryId(1)
                .brand("Tata Tiscon")
                .adjustmentType("percentage_increase")
                .value(BigDecimal.valueOf(3.5))
                .applyTo("both")
                .build();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", true);
        map.put("message", "Bulk price adjustment applied to 4 products");
        map.put("modifiedCount", 4);

        when(sellerPricingService.bulkAdjustPricing(any(), any(BulkPriceAdjustmentRequest.class)))
                .thenReturn(map);

        mockMvc.perform(post("/api/seller/pricing/bulk-adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.modifiedCount").value(4));
    }

    @Test
    @DisplayName("GET & POST /api/seller/documents - Document Vault")
    void documentVault_Success() throws Exception {
        SellerDocumentItemResponse doc = SellerDocumentItemResponse.builder()
                .id("doc_1")
                .documentType("GSTIN")
                .name("GST Registration Certificate")
                .status("APPROVED")
                .uploadedAt(Instant.now())
                .build();

        when(documentVaultService.getSellerDocuments(any())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/seller/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("doc_1"))
                .andExpect(jsonPath("$.data[0].documentType").value("GSTIN"));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
        Map<String, Object> uploadResp = new LinkedHashMap<>();
        uploadResp.put("success", true);
        uploadResp.put("message", "Document uploaded and queued for verification");
        uploadResp.put("data", Map.of("id", "doc_3", "documentType", "MSME", "status", "PENDING"));

        when(documentVaultService.uploadDocument(any(), any(), any()))
                .thenReturn(uploadResp);

        mockMvc.perform(multipart("/api/seller/documents")
                        .file(file)
                        .param("documentType", "MSME"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET & POST /api/seller/enquiries & /api/seller/quotations - Quotations & RFQs")
    void enquiriesAndQuotations_Success() throws Exception {
        SellerEnquiryItemResponse enq = SellerEnquiryItemResponse.builder()
                .id("enq_2026_01")
                .buyerName("L&T Construction Infra Project")
                .projectName("Metro Rail Phase 2")
                .status("NEW")
                .createdAt(LocalDateTime.now())
                .build();

        when(quotationService.getEnquiries(any())).thenReturn(List.of(enq));

        mockMvc.perform(get("/api/seller/enquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("enq_2026_01"));

        SellerQuotationCreateRequest quotReq = SellerQuotationCreateRequest.builder()
                .enquiryId("enq_2026_01")
                .buyerName("L&T Construction Infra Project")
                .items(List.of(SellerQuotationItemDto.builder().productName("Tata Tiscon").quantity(100).quotedRate(BigDecimal.valueOf(61500)).build()))
                .build();

        Map<String, Object> createdResp = new LinkedHashMap<>();
        createdResp.put("success", true);
        createdResp.put("message", "Quotation created and dispatched to buyer successfully");
        createdResp.put("data", Map.of("id", "quot_2026_884", "quotationNumber", "QUOT-HM-2026-884", "status", "SENT", "totalAmount", 7282000));

        when(quotationService.createQuotation(any(), any(SellerQuotationCreateRequest.class)))
                .thenReturn(createdResp);

        mockMvc.perform(post("/api/seller/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quotReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quotationNumber").value("QUOT-HM-2026-884"));

        SellerQuotationRecordResponse rec = SellerQuotationRecordResponse.builder()
                .id("quot_2026_884")
                .quotationNumber("QUOT-HM-2026-884")
                .status("SENT")
                .totalAmount(BigDecimal.valueOf(7282000))
                .build();

        when(quotationService.getQuotations(any())).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/seller/quotations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("quot_2026_884"));
    }
}
