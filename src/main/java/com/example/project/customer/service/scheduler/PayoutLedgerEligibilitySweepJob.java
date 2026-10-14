package com.example.project.customer.service.scheduler;

import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutLedgerEligibilitySweepJob {

    private final SellerPayoutLedgerRepository sellerPayoutLedgerRepository;

    @Value("${payout.ledger.eligibility.sweep.batch-size:50}")
    private int batchSize = 50;

    @Scheduled(fixedDelayString = "${payout.ledger.eligibility.sweep.interval-ms:300000}")
    public void sweepEligibleLedgers() {
        processEligibleLedgers(LocalDateTime.now(), batchSize);
    }

    public int processEligibleLedgers(LocalDateTime cutoff, int batchSize) {
        int effectiveBatchSize = batchSize > 0 ? batchSize : 50;
        int totalUpdated = 0;
        Integer lastId = 0;

        while (true) {
            List<SellerPayoutLedger> candidates = sellerPayoutLedgerRepository.findPendingLedgersEligibleForSettlement(
                    cutoff, lastId, PageRequest.of(0, effectiveBatchSize));

            if (candidates == null || candidates.isEmpty()) {
                break;
            }

            for (SellerPayoutLedger ledger : candidates) {
                lastId = ledger.getId();
                try {
                    int updated = sellerPayoutLedgerRepository.markLedgerEligibleAtomic(ledger.getId(), cutoff);
                    if (updated > 0) {
                        totalUpdated++;
                        log.info("Promoted PayoutLedger #{} (Order #{}) to ELIGIBLE (settlementDate: {})",
                                ledger.getId(), ledger.getOrder() != null ? ledger.getOrder().getOrderId() : null,
                                ledger.getSettlementDate());
                    }
                } catch (Exception e) {
                    log.error("Failed to promote PayoutLedger #{} to ELIGIBLE: {}", ledger.getId(), e.getMessage());
                    // One failure never stops the rest
                }
            }
        }

        if (totalUpdated > 0) {
            log.info("Payout ledger eligibility sweep completed. Promoted {} ledgers to ELIGIBLE.", totalUpdated);
        }
        return totalUpdated;
    }
}
