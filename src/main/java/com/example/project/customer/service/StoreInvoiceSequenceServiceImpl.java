package com.example.project.customer.service;

import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreInvoiceSequence;
import com.example.project.customer.repository.StoreInvoiceSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreInvoiceSequenceServiceImpl implements StoreInvoiceSequenceService {

    private final StoreInvoiceSequenceRepository sequenceRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String generateNextInvoiceNumber(Store store) {
        if (store == null || store.getStoreId() == null) {
            throw new IllegalArgumentException("Store cannot be null when generating sequential invoice number");
        }

        String fy = getCurrentFinancialYear();
        StoreInvoiceSequence sequence = sequenceRepository.findByStoreIdAndFinancialYearForUpdate(store.getStoreId(), fy)
                .orElseGet(() -> {
                    StoreInvoiceSequence newSeq = StoreInvoiceSequence.builder()
                            .store(store)
                            .financialYear(fy)
                            .lastSequenceNumber(0L)
                            .build();
                    return sequenceRepository.save(newSeq);
                });

        long nextNumber = sequence.getLastSequenceNumber() + 1;
        sequence.setLastSequenceNumber(nextNumber);
        sequenceRepository.save(sequence);

        String storeCode = deriveStoreCode(store);
        String formattedInvoiceNumber = String.format("HM/%s/%s/%05d", storeCode, fy, nextNumber);
        log.info("Generated GST invoice #{} for Store #{} ('{}', FY: {})", formattedInvoiceNumber, store.getStoreId(), store.getName(), fy);
        return formattedInvoiceNumber;
    }

    @Override
    public String getCurrentFinancialYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue(); // 1 to 12

        // Indian Financial Year runs from April 1 to March 31
        int startYear = (month >= 4) ? year : (year - 1);
        int endYear = startYear + 1;

        String startYY = String.valueOf(startYear).substring(2);
        String endYY = String.valueOf(endYear).substring(2);
        return startYY + "-" + endYY; // e.g. "24-25"
    }

    private String deriveStoreCode(Store store) {
        if (store.getSlug() != null && !store.getSlug().isBlank()) {
            String clean = store.getSlug().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (!clean.isEmpty()) {
                return clean.length() > 8 ? clean.substring(0, 8) : clean;
            }
        }
        String cleanName = store.getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return cleanName.length() > 8 ? cleanName.substring(0, 8) : cleanName;
    }
}
