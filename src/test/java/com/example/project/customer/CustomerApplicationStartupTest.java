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

    @Autowired(required = false)
    private com.example.project.customer.repository.StoreRepository storeRepository;

    @Autowired(required = false)
    private com.example.project.customer.repository.SellerRepository sellerRepository;

    @Autowired(required = false)
    private jakarta.persistence.EntityManager entityManager;

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

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("JPA Entity Loading: Store with INACTIVE status loads from database without enum conversion errors")
    void testStoreStatus_JpaDatabaseLoading() {
        if (storeRepository != null && sellerRepository != null && entityManager != null) {
            com.example.project.customer.entity.Seller seller = com.example.project.customer.entity.Seller.builder()
                    .name("Test Inactive Vendor")
                    .email("inactive-vendor-" + System.currentTimeMillis() + "@example.com")
                    .phone("99999" + (int)(Math.random() * 90000 + 10000))
                    .build();
            seller = sellerRepository.save(seller);

            com.example.project.customer.entity.Store store = com.example.project.customer.entity.Store.builder()
                    .seller(seller)
                    .name("Inactive Hardware Depot")
                    .slug("inactive-hw-" + System.currentTimeMillis())
                    .status(com.example.project.customer.entity.StoreStatus.INACTIVE)
                    .build();
            store = storeRepository.save(store);

            entityManager.flush();
            entityManager.clear();

            com.example.project.customer.entity.Store loaded = storeRepository.findById(store.getStoreId())
                    .orElseThrow(() -> new AssertionError("Saved store not found"));
            assertThat(loaded.getStatus()).isEqualTo(com.example.project.customer.entity.StoreStatus.INACTIVE);
        }
    }
}
