package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDisputeRequest {

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("description")
    private String description;

    @JsonProperty("itemId")
    private Integer itemId;

    @JsonProperty("photos")
    private List<String> photos;

    @JsonProperty("attachmentUrls")
    private List<String> attachmentUrls;
}
