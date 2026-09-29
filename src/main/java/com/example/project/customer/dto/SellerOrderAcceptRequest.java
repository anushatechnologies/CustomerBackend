package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrderAcceptRequest {

    /**
     * Optional / required vehicle type code selected by the seller based on items weight and size.
     * e.g. "TWO_WHEELER", "THREE_WHEELER", "TATA_ACE", "PICKUP_8FT", "TATA_407"
     */
    private String vehicleTypeCode;
}
