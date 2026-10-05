package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @JsonAlias({"deliveryAddressId", "delivery_address_id", "shippingAddressId", "shipping_address_id"})
    private Integer addressId;

    @Builder.Default
    @JsonAlias({"payment_method"})
    private String paymentMethod = "RAZORPAY";

    @JsonAlias({"delivery_slot"})
    private String deliverySlot;

    @JsonAlias({"delivery_instructions"})
    private String deliveryInstructions;

    @JsonAlias({"po_number"})
    private String poNumber;

    @Builder.Default
    @JsonProperty("requiresCraneUnloading")
    @JsonAlias({"requires_crane_unloading", "craneUnloading", "crane_unloading"})
    private Boolean requiresCraneUnloading = false;

    @JsonProperty("orderForSomeoneElse")
    @JsonAlias({"order_for_someone_else", "orderForOther", "order_for_other", "isGift", "is_gift"})
    private Boolean orderForSomeoneElse;

    @JsonProperty("recipientName")
    @JsonAlias({"recipient_name", "receiverName", "receiver_name", "contactName", "contact_name"})
    private String recipientName;

    @JsonProperty("recipientPhone")
    @JsonAlias({"recipient_phone", "receiverPhone", "receiver_phone", "contactPhone", "contact_phone"})
    private String recipientPhone;
}
