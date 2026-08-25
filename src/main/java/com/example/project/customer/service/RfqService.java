package com.example.project.customer.service;

import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;

import java.util.List;

public interface RfqService {
    RfqResponse createRfq(Integer userId, RfqRequest request);
    PagedResponse<RfqResponse> getRfqs(Integer userId, String status, int page, int limit);
    RfqResponse getRfqById(Integer userId, Integer rfqId);
    List<RfqQuotationDto> getQuotations(Integer userId, Integer rfqId);
    AcceptQuotationResponse acceptQuotation(Integer userId, Integer quoteId);
    List<RfqQuestionDto> getQuestions(Integer userId, Integer rfqId);
    RfqQuestionDto addQuestion(Integer userId, Integer rfqId, RfqQuestionRequest request);
}
