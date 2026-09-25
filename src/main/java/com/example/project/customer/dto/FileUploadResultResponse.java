package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResultResponse {

    @Builder.Default
    private boolean success = true;

    private Integer statusCode;
    private String message;

    @JsonProperty("url")
    private String url;

    @JsonProperty("fileUrl")
    private String fileUrl;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileSize")
    private String fileSize;

    @JsonProperty("sizeBytes")
    private Long sizeBytes;

    @JsonProperty("data")
    private Object data;

    public String getUrl() {
        return url != null ? url : fileUrl;
    }
}
