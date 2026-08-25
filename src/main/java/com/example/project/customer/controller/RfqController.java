package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;
import com.example.project.customer.service.RfqService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rfqs")
public class RfqController {

    private final RfqService rfqService;

    public RfqController(RfqService rfqService) {
        this.rfqService = rfqService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RfqResponse>> createRfq(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @Valid @RequestBody RfqRequest request) {
        Integer uid = userId != null ? userId : 101;
        RfqResponse response = rfqService.createRfq(uid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("RFQ created successfully", response));
    }

    @GetMapping
    public PagedResponse<RfqResponse> getRfqs(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Integer uid = userId != null ? userId : 101;
        return rfqService.getRfqs(uid, status, page, limit);
    }

    @GetMapping("/{id}")
    public ApiResponse<RfqResponse> getRfqById(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(rfqService.getRfqById(uid, id));
    }

    @GetMapping("/{id}/quotations")
    public ApiResponse<List<RfqQuotationDto>> getQuotations(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(rfqService.getQuotations(uid, id));
    }

    @PostMapping("/quotes/{quoteId}/accept")
    public ApiResponse<AcceptQuotationResponse> acceptQuotation(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer quoteId) {
        Integer uid = userId != null ? userId : 101;
        AcceptQuotationResponse response = rfqService.acceptQuotation(uid, quoteId);
        return ApiResponse.ok("Quotation accepted. Order created successfully.", response);
    }

    @GetMapping("/{id}/questions")
    public ApiResponse<List<RfqQuestionDto>> getQuestions(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id) {
        Integer uid = userId != null ? userId : 101;
        return ApiResponse.ok(rfqService.getQuestions(uid, id));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<RfqQuestionDto>> addQuestion(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable Integer id,
            @Valid @RequestBody RfqQuestionRequest request) {
        Integer uid = userId != null ? userId : 101;
        RfqQuestionDto response = rfqService.addQuestion(uid, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Question submitted successfully", response));
    }
}
