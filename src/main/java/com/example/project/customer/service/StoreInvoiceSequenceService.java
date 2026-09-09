package com.example.project.customer.service;

import com.example.project.customer.entity.Store;

public interface StoreInvoiceSequenceService {

    String generateNextInvoiceNumber(Store store);

    String getCurrentFinancialYear();
}
