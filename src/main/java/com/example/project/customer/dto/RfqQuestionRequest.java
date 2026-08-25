package com.example.project.customer.dto;

import jakarta.validation.constraints.NotBlank;

public class RfqQuestionRequest {

    @NotBlank
    private String question;

    public RfqQuestionRequest() {
    }

    public RfqQuestionRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
