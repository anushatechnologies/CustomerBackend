package com.example.project.customer.service.scheduler;

import com.example.project.customer.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HinchStoreRefundReconciliationJob {

    private final PaymentService paymentService;

    @Value("${marketplace.refund.stale-timeout-minutes:15}")
    private int staleTimeoutMinutes;

    @Scheduled(fixedDelayString = "${marketplace.refund.reconciliation-check-ms:300000}")
    public void runReconciliation() {
        try {
            int reconciled = paymentService.reconcileStaleRefunds(staleTimeoutMinutes);
            if (reconciled > 0) {
                log.info("Scheduled refund reconciliation resolved {} stale refunds.", reconciled);
            }
        } catch (Exception e) {
            log.error("Error during scheduled refund reconciliation: {}", e.getMessage(), e);
        }
    }
}
