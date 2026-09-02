package com.example.project.customer.controller;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.SellerDocumentItemResponse;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.service.SellerDocumentVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/documents")
@RequiredArgsConstructor
public class SellerDocumentController {

    private final SellerDocumentVaultService documentVaultService;
    private final SellerContextUtil sellerContextUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerDocumentItemResponse>>> getDocuments() {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        List<SellerDocumentItemResponse> documents = documentVaultService.getSellerDocuments(sellerId);
        return ResponseEntity.ok(ApiResponse.ok("Seller compliance documents retrieved successfully", documents));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("documentType") String documentTypeStr,
            @RequestParam("file") MultipartFile file
    ) {
        Integer sellerId = sellerContextUtil.getCurrentSellerId();
        DocumentType documentType = DocumentType.fromString(documentTypeStr);
        Map<String, Object> result = documentVaultService.uploadDocument(sellerId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
