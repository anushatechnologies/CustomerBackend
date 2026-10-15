package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerOrderDto {

    private Integer id;
    private String orderNumber;
    private String createdAt;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private String paymentMethod;
    private BuyerDto buyer;
    private DeliveryAddressDto deliveryAddress;

    @Builder.Default
    private List<SellerOrderItemDto> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BuyerDto {
        private String name;
        private String phone;
        private String email;
        private String company;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryAddressDto {
        private String completeAddress;
        private String city;
        private String state;
        private String pincode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SellerOrderItemDto {
        private Integer id;
        private Integer productId;
        private String name;
        private String sku;
        private Integer quantity;
        private String unit;
        private BigDecimal price;
        private BigDecimal total;
        private String imageUrl;
        private Integer sellerId;
    }
}
