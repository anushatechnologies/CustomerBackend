package com.example.project.customer.service;

import com.example.project.customer.entity.Estimation;

public interface QuotationPdfService {

    byte[] generateQuotationPdf(Estimation estimation);

    String generateAndUploadQuotationPdf(Estimation estimation);
}
