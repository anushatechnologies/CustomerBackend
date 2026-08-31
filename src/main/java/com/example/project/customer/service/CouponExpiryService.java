package com.example.project.customer.service;

import com.example.project.customer.entity.Coupon;
import com.example.project.customer.entity.CouponStatus;
import com.example.project.customer.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponExpiryService {

    private final CouponRepository couponRepository;

    /**
     * Scheduled job to mark expired coupons.
     * Runs every hour by default (configurable via cron expression).
     * Updates status from ACTIVE to EXPIRED for coupons past their validTo date.
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour at minute 0
    @Transactional
    public void markExpiredCoupons() {
        log.info("Starting scheduled job to mark expired coupons");

        LocalDateTime now = LocalDateTime.now();

        try {
            List<Coupon> expiredCoupons = couponRepository.findExpiredCoupons(now);

            if (expiredCoupons.isEmpty()) {
                log.debug("No expired coupons found");
                return;
            }

            for (Coupon coupon : expiredCoupons) {
                coupon.setStatus(CouponStatus.EXPIRED);
            }

            couponRepository.saveAll(expiredCoupons);
            log.info("Marked {} coupons as expired", expiredCoupons.size());

        } catch (Exception e) {
            log.error("Error marking expired coupons: ", e);
        }
    }

    /**
     * Scheduled job to mark exhausted coupons.
     * Runs every hour by default.
     * Updates status from ACTIVE to EXHAUSTED for coupons that have reached their usage limit.
     */
    @Scheduled(cron = "0 15 * * * *") // Run every hour at minute 15
    @Transactional
    public void markExhaustedCoupons() {
        log.info("Starting scheduled job to mark exhausted coupons");

        try {
            List<Coupon> exhaustedCoupons = couponRepository.findExhaustedCoupons();

            if (exhaustedCoupons.isEmpty()) {
                log.debug("No exhausted coupons found");
                return;
            }

            for (Coupon coupon : exhaustedCoupons) {
                coupon.setStatus(CouponStatus.EXHAUSTED);
            }

            couponRepository.saveAll(exhaustedCoupons);
            log.info("Marked {} coupons as exhausted", exhaustedCoupons.size());

        } catch (Exception e) {
            log.error("Error marking exhausted coupons: ", e);
        }
    }

    /**
     * Manually trigger the expiry check.
     * Can be called from admin endpoints for immediate processing.
     */
    public void triggerExpiryCheck() {
        log.info("Manual trigger of expiry check");
        markExpiredCoupons();
        markExhaustedCoupons();
    }
}
