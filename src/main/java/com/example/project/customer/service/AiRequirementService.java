package com.example.project.customer.service;

import com.example.project.customer.dto.estimation.ExtractedRequirementList;
import org.springframework.web.multipart.MultipartFile;

public interface AiRequirementService {

    ExtractedRequirementList extractRequirements(MultipartFile file, String extractedText, byte[] visionImageBytes);
}
