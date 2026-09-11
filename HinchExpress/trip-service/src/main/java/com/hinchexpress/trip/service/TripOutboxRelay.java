package com.hinchexpress.trip.service;

import com.hinchexpress.trip.entity.DeliveryOutboxEvent;
import com.hinchexpress.trip.repository.DeliveryOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripOutboxRelay {

    private final DeliveryOutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${hinchexpress.kafka.topics.delivery-events:delivery.events}")
    private String deliveryEventsTopic;

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelayString = "${hinchexpress.outbox.relay-interval-ms:2000}")
    @Transactional
    public void relayPendingEvents() {
        List<DeliveryOutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) {
            return;
        }

        for (DeliveryOutboxEvent event : pending) {
            try {
                kafkaTemplate.send(deliveryEventsTopic, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus("PUBLISHED");
                                event.setPublishedAt(LocalDateTime.now());
                                outboxEventRepository.save(event);
                                log.debug("Relayed delivery outbox event [{}] to Kafka topic {}", event.getEventId(), deliveryEventsTopic);
                            } else {
                                event.setRetryCount(event.getRetryCount() + 1);
                                event.setErrorMessage(ex.getMessage());
                                if (event.getRetryCount() >= MAX_RETRIES) {
                                    event.setStatus("FAILED");
                                }
                                outboxEventRepository.save(event);
                                log.error("Failed to relay outbox event [{}] to Kafka: {}", event.getEventId(), ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus("FAILED");
                }
                outboxEventRepository.save(event);
                log.error("Exception sending outbox event [{}] to Kafka: {}", event.getEventId(), e.getMessage());
            }
        }
    }
}

