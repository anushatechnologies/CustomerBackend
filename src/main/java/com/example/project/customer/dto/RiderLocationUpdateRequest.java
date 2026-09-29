package com.example.project.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderLocationUpdateRequest {

    private Double latitude;
    private Double longitude;
    private Boolean isOnline;
    private String fcmToken;
}
