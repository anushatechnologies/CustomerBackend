package com.example.project.customer.service.helper;

import com.example.project.customer.entity.Payment;
import com.example.project.customer.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes refund database status updates in isolated REQUIRES_NEW transactions
 * so that claimed status (REFUND_IN_PROGRESS), failed status (REFUND_PENDING),
 * and success status (REFUNDED) survive any rollbacks in outer business transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundExecutionHelper {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimRefundAtomic(Integer paymentId) {
        if (paymentId == null) {
            return 0;
        }
        return paymentRepository.claimRefundAtomic(paymentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRefundStatus(Integer paymentId, String status, String errorDescription) {
        if (paymentId == null) {
            return;
        }
        paymentRepository.findById(paymentId).ifPresent(p -> {
            p.setStatus(status);
            if (errorDescription != null) {
                p.setErrorDescription(errorDescription);
            }
            paymentRepository.save(p);
            log.info("Committed refund status '{}' for Payment #{} in independent transaction", status, paymentId);
        });
    }
}
