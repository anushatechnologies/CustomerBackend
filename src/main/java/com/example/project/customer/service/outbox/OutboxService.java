package com.example.project.customer.service.outbox;

import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OutboxEvent;

public interface OutboxService {

    OutboxEvent recordOrderConfirmed(Order order);
}
