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
public class CancelledOrderRefundSweepJob {

    private final PaymentService paymentService;

    @Value("${refund.sweep.cancelled-order.age-minutes:10}")
    private int ageMinutes;

    @Value("${refund.sweep.cancelled-order.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${refund.sweep.cancelled-order.interval-ms:300000}")
    public void runSweep() {
        try {
            int swept = paymentService.sweepCancelledOrderRefunds(ageMinutes, batchSize);
            if (swept > 0) {
                log.info("Scheduled cancelled-order refund sweep processed {} payments.", swept);
            }
        } catch (Exception e) {
            log.error("Error during scheduled cancelled-order refund sweep: {}", e.getMessage(), e);
        }
    }
}
