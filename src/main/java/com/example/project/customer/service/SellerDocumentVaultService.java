package com.example.project.customer.service;

import com.example.project.customer.dto.SellerDocumentItemResponse;
import com.example.project.customer.entity.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SellerDocumentVaultService {

    List<SellerDocumentItemResponse> getSellerDocuments(Integer sellerId);

    Map<String, Object> uploadDocument(Integer sellerId, DocumentType documentType, MultipartFile file);
}
