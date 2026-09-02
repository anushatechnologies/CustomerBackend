package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductPageResponse;
import com.example.project.customer.dto.SellerProductUpdateRequest;
import com.example.project.customer.dto.SellerStockUpdateRequest;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.SellerProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerProductController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SellerContextUtil.class})
class SellerProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SellerProductService sellerProductService;

    @Test
    @DisplayName("GET /api/seller/products - Should return paginated seller products")
    void getSellerProducts_Success() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .productId(101)
                .title("Tata Tiscon 550D TMT Steel Rebars 16mm")
                .sku("SKU-TATA-550D-16MM")
                .brandName("Tata Tiscon")
                .price(BigDecimal.valueOf(64500))
                .sellingPrice(BigDecimal.valueOf(64500))
                .mrp(BigDecimal.valueOf(69000))
                .unit("Ton")
                .moq(5)
                .stockQty(85)
                .status("APPROVED")
                .build();

        SellerProductPageResponse pageResp = SellerProductPageResponse.builder()
                .success(true)
                .total(1)
                .page(1)
                .limit(12)
                .data(List.of(prod))
                .build();

        when(sellerProductService.getSellerProducts(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResp);

        mockMvc.perform(get("/api/seller/products")
                        .header("X-Seller-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Tata Tiscon 550D TMT Steel Rebars 16mm"))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /api/seller/products/{id} - Should return single seller product")
    void getSellerProductById_Success() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .productId(101)
                .title("Tata Tiscon 550D 16mm")
                .price(BigDecimal.valueOf(64500))
                .status("APPROVED")
                .build();

        when(sellerProductService.getSellerProductById(any(), eq(101)))
                .thenReturn(prod);

        mockMvc.perform(get("/api/seller/products/sp_101")
                        .header("X-Seller-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Tata Tiscon 550D 16mm"));
    }

    @Test
    @DisplayName("POST /api/seller/products - Should create product with PENDING status and return 201")
    void createSellerProduct_Success() throws Exception {
        SellerProductCreateRequest request = SellerProductCreateRequest.builder()
                .title("Ultratech Super Cement 50kg Bag")
                .sku("SKU-ULTRA-PPC-50KG")
                .brandId(2)
                .unit("Bags")
                .price(BigDecimal.valueOf(395))
                .mrp(BigDecimal.valueOf(440))
                .moq(50)
                .stockQty(400)
                .build();

        ProductResponse created = ProductResponse.builder()
                .productId(102)
                .title(request.getTitle())
                .sku(request.getSku())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(sellerProductService.createSellerProduct(any(), any(SellerProductCreateRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/seller/products")
                        .header("X-Seller-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("sp_102"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/seller/products/{id}/stock - Should fast update stock quantity")
    void patchStockQuantity_Success() throws Exception {
        SellerStockUpdateRequest req = SellerStockUpdateRequest.builder()
                .stockQty(150)
                .build();

        ProductResponse updated = ProductResponse.builder()
                .productId(102)
                .stockQty(150)
                .build();

        when(sellerProductService.updateSellerStock(any(), eq(102), eq(150)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/seller/products/sp_102/stock")
                        .header("X-Seller-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockQty").value(150));
    }

    @Test
    @DisplayName("DELETE /api/seller/products/{id} - Should remove product from seller inventory")
    void deleteSellerProduct_Success() throws Exception {
        doNothing().when(sellerProductService).deleteSellerProduct(any(), eq(102));

        mockMvc.perform(delete("/api/seller/products/sp_102")
                        .header("X-Seller-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product removed from inventory"));
    }
}
