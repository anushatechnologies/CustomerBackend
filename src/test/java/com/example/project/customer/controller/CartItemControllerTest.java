package com.example.project.customer.controller;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.service.CartItemService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartItemController.class)
@Import(GlobalExceptionHandler.class)
class CartItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartItemService cartItemService;

    @Test
    @DisplayName("POST /api/cart-items - Should create cart item and return 201 Created")
    void createCartItem_Success() throws Exception {
        CartItemRequest request = new CartItemRequest(1, 10, 2);
        CartItemResponse response = new CartItemResponse(
                1,
                1,
                "John Doe",
                10,
                "Organic Apple",
                new BigDecimal("2.50"),
                "kg",
                "https://example.com/apple.png",
                2,
                new BigDecimal("5.00"),
                LocalDateTime.now()
        );

        when(cartItemService.create(any(CartItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cart-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartItemId").value(1))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.productTitle").value("Organic Apple"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.price").value(2.50))
                .andExpect(jsonPath("$.itemTotal").value(5.00));
    }

    @Test
    @DisplayName("POST /api/cart-items - Should return 400 Bad Request on invalid input")
    void createCartItem_InvalidInput() throws Exception {
        CartItemRequest request = new CartItemRequest(null, -1, 0);

        mockMvc.perform(post("/api/cart-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    @DisplayName("GET /api/cart-items/{id} - Should return cart item when found")
    void getById_Success() throws Exception {
        CartItemResponse response = new CartItemResponse(
                1,
                1,
                "John Doe",
                10,
                "Organic Apple",
                new BigDecimal("2.50"),
                "kg",
                "https://example.com/apple.png",
                2,
                new BigDecimal("5.00"),
                LocalDateTime.now()
        );

        when(cartItemService.getById(1)).thenReturn(response);

        mockMvc.perform(get("/api/cart-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Organic Apple"));
    }

    @Test
    @DisplayName("GET /api/cart-items/{id} - Should return 404 when not found")
    void getById_NotFound() throws Exception {
        when(cartItemService.getById(99)).thenThrow(new ResourceNotFoundException("Cart item not found with id: 99"));

        mockMvc.perform(get("/api/cart-items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cart item not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/cart-items - Should return list of cart items")
    void getAll_Success() throws Exception {
        CartItemResponse item = new CartItemResponse(
                1, 1, "John", 10, "Apple", new BigDecimal("2.50"), "kg", null, 2, new BigDecimal("5.00"), LocalDateTime.now()
        );

        when(cartItemService.getAll(null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/cart-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cartItemId").value(1));
    }

    @Test
    @DisplayName("GET /api/cart-items/customer/{customerId} - Should return customer cart items")
    void getByCustomerId_Success() throws Exception {
        CartItemResponse item = new CartItemResponse(
                1, 1, "John", 10, "Apple", new BigDecimal("2.50"), "kg", null, 2, new BigDecimal("5.00"), LocalDateTime.now()
        );

        when(cartItemService.getByCustomerId(1)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/cart-items/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }

    @Test
    @DisplayName("PUT /api/cart-items/{id} - Should update cart item and return updated")
    void update_Success() throws Exception {
        CartItemRequest request = new CartItemRequest(1, 10, 4);
        CartItemResponse response = new CartItemResponse(
                1, 1, "John", 10, "Apple", new BigDecimal("2.50"), "kg", null, 4, new BigDecimal("10.00"), LocalDateTime.now()
        );

        when(cartItemService.update(eq(1), any(CartItemRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/cart-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.itemTotal").value(10.00));
    }

    @Test
    @DisplayName("PUT /api/cart-items/{id} - Should return 409 Conflict when item has conflict")
    void update_Conflict() throws Exception {
        CartItemRequest request = new CartItemRequest(1, 10, 4);

        when(cartItemService.update(eq(1), any(CartItemRequest.class)))
                .thenThrow(new ResourceConflictException("Stock exceeded"));

        mockMvc.perform(put("/api/cart-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock exceeded"));
    }

    @Test
    @DisplayName("DELETE /api/cart-items/{id} - Should delete cart item and return 204")
    void delete_Success() throws Exception {
        doNothing().when(cartItemService).delete(1);

        mockMvc.perform(delete("/api/cart-items/1"))
                .andExpect(status().isNoContent());

        verify(cartItemService).delete(1);
    }

    @Test
    @DisplayName("DELETE /api/cart-items/customer/{customerId} - Should clear customer cart and return 204")
    void deleteByCustomerId_Success() throws Exception {
        doNothing().when(cartItemService).deleteByCustomerId(1);

        mockMvc.perform(delete("/api/cart-items/customer/1"))
                .andExpect(status().isNoContent());

        verify(cartItemService).deleteByCustomerId(1);
    }
}
