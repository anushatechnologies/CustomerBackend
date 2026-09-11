package com.example.project.customer;

import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.event.OrderConfirmedEvent;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.OutboxEvent;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.OutboxEventRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreInvoiceSequenceRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CheckoutService;
import com.example.project.customer.service.OrderServiceImpl;
import com.example.project.customer.service.StoreInvoiceSequenceService;
import com.example.project.customer.service.outbox.OutboxEventRelay;
import com.example.project.customer.service.outbox.OutboxService;
import com.example.project.customer.service.outbox.OutboxServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class HichExpressIntegrationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private StoreInvoiceSequenceService storeInvoiceSequenceService;

    @Mock
    private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboxServiceImpl outboxService;
    private OutboxEventRelay outboxEventRelay;
    private OrderServiceImpl orderService;

    private Store mockStore;
    private Address mockAddress;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxService = new OutboxServiceImpl(outboxEventRepository, addressRepository, storeRepository, objectMapper);

        outboxEventRelay = new OutboxEventRelay(outboxEventRepository);
        ReflectionTestUtils.setField(outboxEventRelay, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(outboxEventRelay, "orderConfirmedTopic", "order.confirmed");

        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                productRepository,
                addressRepository,
                customerRepository,
                cartService,
                checkoutService,
                null,
                storeInvoiceSequenceService,
                sellerPayoutLedgerRepository,
                storeRepository,
                null,
                null
        );

        mockStore = Store.builder()
                .storeId(1)
                .name("HinchStore Hyderabad Central")
                .slug("hinchstore-hyd")
                .status(StoreStatus.ACTIVE)
                .latitude(17.385044)
                .longitude(78.486671)
                .build();

        mockAddress = Address.builder()
                .id(25)
                .siteName("Gachibowli Site A")
                .addressLine1("Plot 12, Financial District")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500032")
                .latitude(17.448294)
                .longitude(78.391485)
                .build();

        product1 = Product.builder()
                .productId(101)
                .title("Ultratech Cement 50kg")
                .price(BigDecimal.valueOf(380.00))
                .stockQty(500)
                .active(true)
                .weightKg(BigDecimal.valueOf(50.000))
                .build();

        product2 = Product.builder()
                .productId(102)
                .title("TMT Steel Rebar 12mm")
                .price(BigDecimal.valueOf(650.00))
                .stockQty(200)
                .active(true)
                .weightKg(BigDecimal.valueOf(10.500))
                .build();
    }

    @Test
    @DisplayName("1. Product entity: weightKg persistence and field verification")
    void testProductWeightKgField() {
        Product p = Product.builder()
                .productId(1)
                .title("Test Heavy Aggregate")
                .weightKg(new BigDecimal("150.750"))
                .build();

        assertThat(p.getWeightKg()).isEqualByComparingTo("150.750");
    }

    @Test
    @DisplayName("2. Store entity: pickup GPS coordinates (latitude/longitude) representation")
    void testStorePickupCoordinates() {
        Store s = Store.builder()
                .storeId(10)
                .name("Kukatpally Depot")
                .latitude(17.4933)
                .longitude(78.3914)
                .build();

        assertThat(s.getLatitude()).isEqualTo(17.4933);
        assertThat(s.getLongitude()).isEqualTo(78.3914);
    }

    @Test
    @DisplayName("3. Order creation: snapshots Product.weightKg into OrderItem and calculates Order.totalWeightKg")
    void testOrderCreation_WeightSnapshotAndTotalWeightCalculation() {
        int userId = 42;

        CartItemResponse item1 = CartItemResponse.builder()
                .productId(101)
                .title("Ultratech Cement 50kg")
                .quantity(4) // 4 * 50.000 = 200.000 kg
                .unit("BAG")
                .unitPrice(BigDecimal.valueOf(380.00))
                .lineTotal(BigDecimal.valueOf(1520.00))
                .lineGst(BigDecimal.valueOf(273.60))
                .build();

        CartItemResponse item2 = CartItemResponse.builder()
                .productId(102)
                .title("TMT Steel Rebar 12mm")
                .quantity(2) // 2 * 10.500 = 21.000 kg
                .unit("PIECE")
                .unitPrice(BigDecimal.valueOf(650.00))
                .lineTotal(BigDecimal.valueOf(1300.00))
                .lineGst(BigDecimal.valueOf(234.00))
                .build();

        CartResponse cart = CartResponse.builder()
                .storeId(1)
                .items(List.of(item1, item2))
                .subtotal(BigDecimal.valueOf(2820.00))
                .grandTotal(BigDecimal.valueOf(3327.60))
                .build();

        when(cartService.getCart(userId)).thenReturn(cart);
        when(storeRepository.findById(1)).thenReturn(Optional.of(mockStore));
        when(productRepository.findById(101)).thenReturn(Optional.of(product1));
        when(productRepository.findById(102)).thenReturn(Optional.of(product2));
        when(productRepository.findByIdForStockUpdate(101)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdForStockUpdate(102)).thenReturn(Optional.of(product2));
        when(addressRepository.findByCustomer_CustomerIdAndId(userId, 25)).thenReturn(Optional.of(mockAddress));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .taxableAmount(BigDecimal.valueOf(2820.00))
                .subtotal(BigDecimal.valueOf(2820.00))
                .discount(BigDecimal.ZERO)
                .cgst(BigDecimal.valueOf(253.80))
                .sgst(BigDecimal.valueOf(253.80))
                .igst(BigDecimal.ZERO)
                .totalGst(BigDecimal.valueOf(507.60))
                .freightCharge(BigDecimal.ZERO)
                .craneUnloadingCharge(BigDecimal.ZERO)
                .grandTotal(BigDecimal.valueOf(3327.60))
                .build();
        when(checkoutService.previewCheckout(eq(userId), any(CheckoutPreviewRequest.class))).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(mockStore)).thenReturn("INV-2026-0001");

        // Mock saving Order
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order arg = invocation.getArgument(0);
            if (arg.getOrderId() == null) {
                arg.setOrderId(501);
            }
            return arg;
        });

        // Mock saving OrderItem
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem oi = invocation.getArgument(0);
            oi.setOrderItemId(1001);
            return oi;
        });

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(25)
                .paymentMethod("RAZORPAY")
                .build();

        OrderResponse response = orderService.createOrder(userId, request);

        assertThat(response).isNotNull();
        // Verify Order totalWeightKg: 4 * 50.000 + 2 * 10.500 = 221.000 kg
        assertThat(response.getTotalWeightKg()).isEqualByComparingTo("221.000");

        // Verify OrderItem snapshot captor
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(2)).save(itemCaptor.capture());

        List<OrderItem> savedItems = itemCaptor.getAllValues();
        assertThat(savedItems).hasSize(2);

        OrderItem savedItem1 = savedItems.get(0);
        assertThat(savedItem1.getProductId()).isEqualTo(101);
        assertThat(savedItem1.getWeightKg()).isEqualByComparingTo("50.000");

        OrderItem savedItem2 = savedItems.get(1);
        assertThat(savedItem2.getProductId()).isEqualTo(102);
        assertThat(savedItem2.getWeightKg()).isEqualByComparingTo("10.500");
    }

    @Test
    @DisplayName("4. OutboxService: creates transactional OutboxEvent with correct OrderConfirmed contract")
    void testOutboxService_RecordOrderConfirmed() throws Exception {
        Order confirmedOrder = Order.builder()
                .orderId(501)
                .customer(Customer.builder().customerId(85).build())
                .store(mockStore)
                .addressId(25)
                .totalWeightKg(new BigDecimal("221.000"))
                .orderStatus("CONFIRMED")
                .build();

        when(outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType("ORDER", "501", "ORDER_CONFIRMED"))
                .thenReturn(false);
        when(addressRepository.findById(25)).thenReturn(Optional.of(mockAddress));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent oe = invocation.getArgument(0);
            oe.setId(1L);
            return oe;
        });

        OutboxEvent outboxEvent = outboxService.recordOrderConfirmed(confirmedOrder);

        assertThat(outboxEvent).isNotNull();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("ORDER");
        assertThat(outboxEvent.getAggregateId()).isEqualTo("501");
        assertThat(outboxEvent.getEventType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(outboxEvent.getStatus()).isEqualTo("PENDING");

        // Parse and verify payload JSON structure
        JsonNode json = objectMapper.readTree(outboxEvent.getPayload());
        assertThat(json.get("orderId").asInt()).isEqualTo(501);
        assertThat(json.get("customerId").asInt()).isEqualTo(85);
        assertThat(json.get("storeId").asInt()).isEqualTo(1);
        assertThat(json.get("pickupLatitude").asDouble()).isEqualTo(17.385044);
        assertThat(json.get("pickupLongitude").asDouble()).isEqualTo(78.486671);
        assertThat(json.get("deliveryAddressId").asInt()).isEqualTo(25);
        assertThat(json.get("deliveryLatitude").asDouble()).isEqualTo(17.448294);
        assertThat(json.get("deliveryLongitude").asDouble()).isEqualTo(78.391485);
        assertThat(new BigDecimal(json.get("totalWeightKg").asText())).isEqualByComparingTo("221.000");
    }

    @Test
    @DisplayName("5. Idempotency: duplicate payment confirmation attempts do NOT create duplicate outbox records")
    void testOutboxService_IdempotencyDuplicatePrevented() {
        Order confirmedOrder = Order.builder()
                .orderId(501)
                .customer(Customer.builder().customerId(85).build())
                .store(mockStore)
                .addressId(25)
                .totalWeightKg(new BigDecimal("221.000"))
                .orderStatus("CONFIRMED")
                .build();

        // 1st attempt: outbox does not exist yet -> created
        when(outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType("ORDER", "501", "ORDER_CONFIRMED"))
                .thenReturn(false);
        when(addressRepository.findById(25)).thenReturn(Optional.of(mockAddress));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent oe = invocation.getArgument(0);
            oe.setId(1L);
            return oe;
        });

        OutboxEvent firstCall = outboxService.recordOrderConfirmed(confirmedOrder);
        assertThat(firstCall).isNotNull();

        // 2nd attempt (e.g. Razorpay webhook after frontend verifyPayment already ran):
        // existsByAggregateTypeAndAggregateIdAndEventType now returns true
        when(outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType("ORDER", "501", "ORDER_CONFIRMED"))
                .thenReturn(true);

        OutboxEvent secondCall = outboxService.recordOrderConfirmed(confirmedOrder);
        assertThat(secondCall).isNull();

        // Verify outboxEventRepository.save was called exactly ONCE across both invocations
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("6. Concurrency Guard: handles DataIntegrityViolationException on race condition gracefully")
    void testOutboxService_RaceConditionDbUniqueConstraint() {
        Order order = Order.builder()
                .orderId(502)
                .customer(Customer.builder().customerId(85).build())
                .store(mockStore)
                .addressId(25)
                .build();

        when(outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType("ORDER", "502", "ORDER_CONFIRMED"))
                .thenReturn(false);
        when(addressRepository.findById(25)).thenReturn(Optional.of(mockAddress));
        when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'ORDER-502-ORDER_CONFIRMED' for key 'uk_outbox_aggregate_event'"));

        // Must not throw, but return null gracefully
        OutboxEvent result = outboxService.recordOrderConfirmed(order);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("7. OutboxEventRelay: publishes PENDING event to Kafka topic order.confirmed and transitions to PUBLISHED")
    void testOutboxRelay_PublishesToKafka() {
        OutboxEvent pendingEvent = OutboxEvent.builder()
                .id(100L)
                .aggregateType("ORDER")
                .aggregateId("501")
                .eventType("ORDER_CONFIRMED")
                .payload("{\"orderId\":501,\"totalWeightKg\":221.000}")
                .status("PENDING")
                .retryCount(0)
                .build();

        when(kafkaTemplate.send(eq("order.confirmed"), eq("501"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        boolean published = outboxEventRelay.publishEvent(pendingEvent);

        assertThat(published).isTrue();
        assertThat(pendingEvent.getStatus()).isEqualTo("PUBLISHED");
        assertThat(pendingEvent.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(pendingEvent);
    }

    @Test
    @DisplayName("8. Flyway migration: verify V10 migration file exists and contains all required schema changes")
    void testFlywayV10MigrationFileIntegrity() throws Exception {
        Path migrationPath = Paths.get("src/main/resources/db/migration/V10__add_hichexpress_delivery_integration_and_outbox.sql");
        assertThat(Files.exists(migrationPath)).isTrue();

        String sql = Files.readString(migrationPath);
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS outbox_events");
        assertThat(sql).contains("uk_outbox_aggregate_event");
        assertThat(sql).contains("products");
        assertThat(sql).contains("weight_kg");
        assertThat(sql).contains("order_items");
        assertThat(sql).contains("orders");
        assertThat(sql).contains("total_weight_kg");
        assertThat(sql).contains("stores");
        assertThat(sql).contains("latitude");
        assertThat(sql).contains("longitude");
    }
}
