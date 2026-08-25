package com.example.project.customer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RfqQuestionDto {

    private Integer questionId;
    private Integer rfqId;
    private String question;
    private String response;
    private String status;
    private LocalDateTime createdAt;

    public RfqQuestionDto() {
    }

    public RfqQuestionDto(Integer questionId, Integer rfqId, String question, String response, String status, LocalDateTime createdAt) {
        this.questionId = questionId;
        this.rfqId = rfqId;
        this.question = question;
        this.response = response;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getRfqId() {
        return rfqId;
    }

    public void setRfqId(Integer rfqId) {
        this.rfqId = rfqId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
