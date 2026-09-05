package com.example.project.customer.controller;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.QuotationRequest;
import com.example.project.customer.dto.QuotationResponse;
import com.example.project.customer.dto.RfqQuestionRequest;
import com.example.project.customer.dto.RfqQuestionResponse;
import com.example.project.customer.dto.RfqRequest;
import com.example.project.customer.dto.RfqResponse;
import com.example.project.customer.service.RfqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rfqs")
@RequiredArgsConstructor
public class RfqController {

    private final RfqService rfqService;
    private final UserContextUtil userContextUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<RfqResponse>> createRfq(@Valid @RequestBody RfqRequest request) {
        Integer userId = userContextUtil.getCurrentUserId();
        RfqResponse rfq = rfqService.createRfq(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("RFQ created successfully", rfq));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RfqResponse>>> getRfqs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        Integer userId = userContextUtil.getCurrentUserId();
        ApiResponse<List<RfqResponse>> response = rfqService.getRfqs(userId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RfqResponse>> getRfqById(@PathVariable Integer id) {
        RfqResponse rfq = rfqService.getRfqById(id);
        return ResponseEntity.ok(ApiResponse.ok("RFQ retrieved successfully", rfq));
    }

    @GetMapping("/{id}/quotations")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getRfqQuotations(@PathVariable Integer id) {
        List<QuotationResponse> quotations = rfqService.getRfqQuotations(id);
        return ResponseEntity.ok(ApiResponse.ok("Quotations retrieved successfully", quotations));
    }

    @PostMapping("/{id}/quotations")
    public ResponseEntity<ApiResponse<QuotationResponse>> addQuotation(
            @PathVariable Integer id,
            @Valid @RequestBody QuotationRequest request) {
        QuotationResponse quotation = rfqService.addQuotation(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Quotation submitted successfully", quotation));
    }

    @PostMapping("/quotes/{id}/accept")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptQuotation(@PathVariable Integer id) {
        Map<String, Object> result = rfqService.acceptQuotation(id);
        return ResponseEntity.ok(ApiResponse.ok("Quotation accepted. Order created successfully.", result));
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<List<RfqQuestionResponse>>> getRfqQuestions(@PathVariable Integer id) {
        List<RfqQuestionResponse> questions = rfqService.getRfqQuestions(id);
        return ResponseEntity.ok(ApiResponse.ok("RFQ clarification questions retrieved successfully", questions));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<RfqQuestionResponse>> addRfqQuestion(
            @PathVariable Integer id,
            @Valid @RequestBody RfqQuestionRequest request) {
        RfqQuestionResponse created = rfqService.addRfqQuestion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Question submitted successfully", created));
    }

    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<ApiResponse<RfqQuestionResponse>> answerRfqQuestion(
            @PathVariable Integer questionId,
            @RequestBody Map<String, String> body) {
        String answer = body.getOrDefault("response", "");
        RfqQuestionResponse answered = rfqService.answerRfqQuestion(questionId, answer);
        return ResponseEntity.ok(ApiResponse.ok("Question answered successfully", answered));
    }
}
