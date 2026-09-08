package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendEmailRequest {

    private String from;

    private List<String> to;

    private String subject;

    private String html;

    private String text;

    @JsonProperty("reply_to")
    private String replyTo;
}
