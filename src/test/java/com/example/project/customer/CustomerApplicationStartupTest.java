package com.example.project.customer;

import com.example.project.CustomerApplication;
import com.example.project.customer.repository.OrderGroupRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.PaymentRepository;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CheckoutService;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CustomerApplication.class)
@ActiveProfiles("dev")
public class CustomerApplicationStartupTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private CartService cartService;

    @Autowired(required = false)
    private CheckoutService checkoutService;

    @Autowired(required = false)
    private OrderService orderService;

    @Autowired(required = false)
    private PaymentService paymentService;

    @Autowired(required = false)
    private OrderGroupRepository orderGroupRepository;

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Application starts up cleanly and wires all multi-store cart & checkout components")
    void contextLoadsAndWiresMultiStoreComponents() {
        assertThat(applicationContext).isNotNull();
        assertThat(cartService).isNotNull();
        assertThat(checkoutService).isNotNull();
        assertThat(orderService).isNotNull();
        assertThat(paymentService).isNotNull();
        assertThat(orderGroupRepository).isNotNull();
        assertThat(orderRepository).isNotNull();
        assertThat(paymentRepository).isNotNull();
    }
}
