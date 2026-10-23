package com.example.project.customer.service.scheduler;

import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderGroup;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.repository.OrderGroupRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class HinchStoreOrderTimeoutJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PlatformMarketplaceProperties platformMarketplaceProperties;
    private OrderGroupRepository orderGroupRepository;

    @Autowired
    public HinchStoreOrderTimeoutJob(
            OrderRepository orderRepository,
            OrderService orderService,
            PlatformMarketplaceProperties platformMarketplaceProperties,
            @Autowired(required = false) OrderGroupRepository orderGroupRepository) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.platformMarketplaceProperties = platformMarketplaceProperties;
        this.orderGroupRepository = orderGroupRepository;
    }

    public HinchStoreOrderTimeoutJob(
            OrderRepository orderRepository,
            OrderService orderService,
            PlatformMarketplaceProperties platformMarketplaceProperties) {
        this(orderRepository, orderService, platformMarketplaceProperties, null);
    }

    public void setOrderGroupRepository(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

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

                // 4. Update parent OrderGroup if present
                if (order.getOrderGroup() != null && orderGroupRepository != null) {
                    OrderGroup group = order.getOrderGroup();
                    List<Order> siblings = orderRepository.findByOrderGroup_Id(group.getId());
                    boolean allCancelledOrExpired = siblings.isEmpty() || siblings.stream().allMatch(
                            s -> "CANCELLED".equalsIgnoreCase(s.getOrderStatus()) || "EXPIRED".equalsIgnoreCase(s.getPaymentStatus()));
                    if (allCancelledOrExpired) {
                        group.setGroupStatus("CANCELLED");
                        group.setPaymentStatus("EXPIRED");
                    } else {
                        group.setGroupStatus("PARTIALLY_CANCELLED");
                    }
                    orderGroupRepository.save(group);
                }

                log.info("Successfully cancelled expired HinchStore Order #{} (Invoice: {}) and released inventory.",
                        order.getOrderId(), order.getStoreInvoiceNumber());
            } catch (Exception e) {
                log.error("Failed to cancel expired HinchStore Order #{}: {}", order.getOrderId(), e.getMessage(), e);
            }
        }
    }
}
