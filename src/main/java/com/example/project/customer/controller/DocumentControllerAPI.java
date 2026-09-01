package com.example.project.customer.controller;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.SellerDocumentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/seller/documents")
public class DocumentControllerAPI {

    /**
     * GET /api/seller/documents
     * List uploaded seller compliance documents
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDocuments() {
        // TODO: Implement document retrieval logic
        List<SellerDocumentResponse> documents = new ArrayList<>();
        
        PaginationMeta pagination = PaginationMeta.builder()
                .totalCount(0L)
                .page(1)
                .limit(50)
                .totalPages(0)
                .build();
        
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Documents retrieved successfully")
                .data(documents)
                .pagination(pagination)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/seller/documents
     * Upload compliance document to vault
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> uploadDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        // TODO: Implement document upload logic with validation
        ApiResponse<?> response = ApiResponse.builder()
                .success(true)
                .statusCode(201)
                .message("Document uploaded and queued for verification")
                .data(null)
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
