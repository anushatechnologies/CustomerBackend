package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionalVideoResponse {

    private Integer id;
    private String videoUrl;
    private String posterUrl;
    private String title;
    private String subtitle;
    private String badge;
    private String ctaText;
    private String targetScreen;

    @JsonProperty("isActive")
    private Boolean active;

    @JsonProperty("isActive")
    public Boolean getActive() {
        return active;
    }

    @JsonProperty("isActive")
    public void setActive(Boolean active) {
        this.active = active;
    }
}
