package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PincodeServiceabilityResponse {

    @Builder.Default
    @JsonProperty("success")
    private Boolean success = true;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("pincode")
    private String pincode;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("serviceable")
    @Builder.Default
    private boolean serviceable = true;

    @JsonProperty("estimatedDays")
    @Builder.Default
    private Integer estimatedDays = 2;

    @JsonProperty("isExpressAvailable")
    @Builder.Default
    private Boolean isExpressAvailable = true;

    @JsonProperty("area")
    private String area;

    @JsonProperty("district")
    private String district;

    @JsonProperty("data")
    public Map<String, Object> getData() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pincode", pincode != null ? pincode : "");
        map.put("city", city != null ? city : "");
        map.put("state", state != null ? state : "");
        map.put("serviceable", serviceable);
        map.put("estimatedDays", estimatedDays != null ? estimatedDays : 2);
        map.put("isExpressAvailable", isExpressAvailable != null ? isExpressAvailable : true);
        if (area != null) map.put("area", area);
        if (district != null) map.put("district", district);
        return map;
    }
}
