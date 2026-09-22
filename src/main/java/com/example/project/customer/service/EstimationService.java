package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.estimation.EstimationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EstimationService {

    EstimationResponse uploadAndProcess(Integer customerId, MultipartFile file);

    EstimationResponse getEstimation(Integer customerId, Long estimationId);

    EstimationResponse resolveItemMatch(Integer customerId, Long estimationId, Long itemId, Integer productId);

    EstimationResponse generateQuotation(Integer customerId, Long estimationId);

    byte[] downloadQuotationPdf(Integer customerId, Long estimationId);

    ApiResponse<List<EstimationResponse>> getCustomerEstimations(Integer customerId, int page, int limit);
}
