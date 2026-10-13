package com.example.project.customer.service.scheduler;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HinchStoreOrderTimeoutJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PlatformMarketplaceProperties platformMarketplaceProperties;

    @Value("${marketplace.hinch-store.unpaid-timeout-minutes:15}")
    private int unpaidTimeoutMinutes;

    @Scheduled(fixedDelayString = "${marketplace.hinch-store.timeout-check-ms:60000}")
    @Transactional
    public void cancelExpiredUnpaidHinchOrders() {
        Integer platformStoreId = platformMarketplaceProperties != null ? platformMarketplaceProperties.getPlatformStoreId() : 1;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(unpaidTimeoutMinutes);

        List<Order> expiredOrders = orderRepository.findExpiredUnpaidHinchOrders(platformStoreId, cutoff);
        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired unpaid HinchStore orders older than {} minutes. Processing expiration & release.",
                expiredOrders.size(), unpaidTimeoutMinutes);

        for (Order order : expiredOrders) {
            try {
                // 1. Release reserved consignment inventory & reverse ledgers
                orderService.releaseOrRestockHinchStoreInventory(order);

                // 2. Transition order & payment status
                order.setOrderStatus("CANCELLED");
                order.setPaymentStatus("EXPIRED");

                // 3. Append tracking checkpoint
                TrackingCheckpoint timeoutCheckpoint = TrackingCheckpoint.builder()
                        .order(order)
                        .status("PAYMENT_EXPIRED")
                        .title("Order Cancelled")
                        .location("HinchMart Central System")
                        .description("Order automatically cancelled because payment was not completed within " + unpaidTimeoutMinutes + " minutes.")
                        .timestamp(LocalDateTime.now())
                        .build();
                order.getCheckpoints().add(timeoutCheckpoint);

                orderRepository.save(order);
                log.info("Successfully cancelled expired HinchStore Order #{} (Invoice: {}) and released inventory.",
                        order.getOrderId(), order.getStoreInvoiceNumber());
            } catch (Exception e) {
                log.error("Failed to cancel expired HinchStore Order #{}: {}", order.getOrderId(), e.getMessage(), e);
            }
        }
    }
}
