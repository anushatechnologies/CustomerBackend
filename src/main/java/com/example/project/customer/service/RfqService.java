package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.QuotationRequest;
import com.example.project.customer.dto.QuotationResponse;
import com.example.project.customer.dto.RfqQuestionRequest;
import com.example.project.customer.dto.RfqQuestionResponse;
import com.example.project.customer.dto.RfqRequest;
import com.example.project.customer.dto.RfqResponse;

import java.util.List;
import java.util.Map;

public interface RfqService {
    RfqResponse createRfq(Integer userId, RfqRequest request);
    ApiResponse<List<RfqResponse>> getRfqs(Integer userId, String status, int page, int limit);
    RfqResponse getRfqById(Integer id);
    List<QuotationResponse> getRfqQuotations(Integer rfqId);
    QuotationResponse addQuotation(Integer rfqId, QuotationRequest request);
    Map<String, Object> acceptQuotation(Integer quoteId);
    List<RfqQuestionResponse> getRfqQuestions(Integer rfqId);
    RfqQuestionResponse addRfqQuestion(Integer rfqId, RfqQuestionRequest request);
    RfqQuestionResponse answerRfqQuestion(Integer questionId, String responseText);
}
