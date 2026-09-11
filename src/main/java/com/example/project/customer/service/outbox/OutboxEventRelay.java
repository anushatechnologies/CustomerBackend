package com.example.project.customer.service.outbox;

import com.example.project.customer.entity.OutboxEvent;
import com.example.project.customer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventRelay {

    private final OutboxEventRepository outboxEventRepository;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.order-confirmed:order.confirmed}")
    private String orderConfirmedTopic;

    private static final int MAX_RETRIES = 10;
    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:3000}")
    @Transactional
    public void relayPendingEvents() {
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate is not available. Skipping outbox relay cycle.");
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                "PENDING", PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox event(s) to relay to Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
    }

    public boolean publishEvent(OutboxEvent event) {
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate unavailable. Cannot publish outbox event #{}", event.getId());
            return false;
        }

        try {
            log.info("Publishing OutboxEvent #{} (Aggregate: {}:{}) to Kafka topic '{}'",
                    event.getId(), event.getAggregateType(), event.getAggregateId(), orderConfirmedTopic);

            kafkaTemplate.send(orderConfirmedTopic, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);

            event.setStatus("PUBLISHED");
            event.setProcessedAt(LocalDateTime.now());
            event.setErrorMessage(null);
            outboxEventRepository.save(event);

            log.info("Successfully published OutboxEvent #{} to Kafka topic '{}'", event.getId(), orderConfirmedTopic);
            return true;
        } catch (Exception e) {
            int newRetryCount = (event.getRetryCount() != null ? event.getRetryCount() : 0) + 1;
            event.setRetryCount(newRetryCount);
            event.setErrorMessage(e.getMessage());

            if (newRetryCount >= MAX_RETRIES) {
                event.setStatus("FAILED");
                log.error("OutboxEvent #{} reached max retries ({}) and is marked FAILED: {}",
                        event.getId(), MAX_RETRIES, e.getMessage());
            } else {
                log.warn("Failed to publish OutboxEvent #{} (attempt {}/{}): {}",
                        event.getId(), newRetryCount, MAX_RETRIES, e.getMessage());
            }

            outboxEventRepository.save(event);
            return false;
        }
    }
}
