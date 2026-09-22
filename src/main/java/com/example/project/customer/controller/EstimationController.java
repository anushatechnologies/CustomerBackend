package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.estimation.EstimationResponse;
import com.example.project.customer.dto.estimation.ResolveProductRequest;
import com.example.project.customer.security.FirebaseUserPrincipal;
import com.example.project.customer.service.EstimationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/estimations")
@RequiredArgsConstructor
public class EstimationController {

    private final EstimationService estimationService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EstimationResponse>> uploadAndProcess(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer customerIdHeader,
            Authentication authentication) {

        Integer customerId = resolveCustomerId(customerIdHeader, authentication);
        log.info("Received requirement upload request: customerId={}, fileName={}, size={}",
                customerId, file.getOriginalFilename(), file.getSize());

        EstimationResponse response = estimationService.uploadAndProcess(customerId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Requirement document uploaded and processed successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EstimationResponse>> getEstimation(
            @PathVariable Long id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer customerIdHeader,
            Authentication authentication) {

        Integer customerId = resolveCustomerId(customerIdHeader, authentication);
        EstimationResponse response = estimationService.getEstimation(customerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Estimation retrieved successfully", response));
    }

    @PostMapping("/{id}/items/{itemId}/resolve")
    public ResponseEntity<ApiResponse<EstimationResponse>> resolveItemMatch(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody ResolveProductRequest request,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer customerIdHeader,
            Authentication authentication) {

        Integer customerId = resolveCustomerId(customerIdHeader, authentication);
        EstimationResponse response = estimationService.resolveItemMatch(customerId, id, itemId, request.getProductId());
        return ResponseEntity.ok(ApiResponse.ok("Selected product applied and estimation recalculated successfully", response));
    }

    @PostMapping("/{id}/generate-quotation")
    public ResponseEntity<ApiResponse<EstimationResponse>> generateQuotation(
            @PathVariable Long id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer customerIdHeader,
            Authentication authentication) {

        Integer customerId = resolveCustomerId(customerIdHeader, authentication);
        EstimationResponse response = estimationService.generateQuotation(customerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Quotation PDF generated and stored successfully", response));
    }

    @GetMapping("/{id}/quotation/download")
    public ResponseEntity<byte[]> downloadQuotationPdf(
            @PathVariable Long id,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer customerIdHeader,
            Authentication authentication) {

        Integer customerId = resolveCustomerId(customerIdHeader, authentication);
        byte[] pdfBytes = estimationService.downloadQuotationPdf(customerId, id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "HinchMart-Quotation-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EstimationResponse>>> getEstimations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "X-Customer-Id", required = false) Integer customerIdHeader,
            Authentication authentication) {

        Integer customerId = resolveCustomerId(customerIdHeader, authentication);
        ApiResponse<List<EstimationResponse>> response = estimationService.getCustomerEstimations(customerId, page, limit);
        return ResponseEntity.ok(response);
    }

    private Integer resolveCustomerId(Integer customerIdHeader, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof FirebaseUserPrincipal principal) {
            if (principal.getInternalUserId() != null) {
                return principal.getInternalUserId();
            }
        }
        if (customerIdHeader != null && customerIdHeader > 0) {
            return customerIdHeader;
        }
        return 101;
    }
}
