package com.example.project.customer.service;

import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.PaymentOrderCreateRequest;
import com.example.project.customer.dto.PaymentOrderCreateResponse;
import com.example.project.customer.dto.PaymentStatusResponse;
import com.example.project.customer.dto.PaymentVerifyRequest;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.Payment;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.PaymentRepository;
import com.example.project.customer.repository.WalletRepository;
import com.example.project.customer.repository.WalletTransactionRepository;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private OrderService orderService;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "keyId", "rzp_test_mockKey");
        ReflectionTestUtils.setField(paymentService, "keySecret", "mockSecretKey123");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "mockWebhookSecret");

        customer = Customer.builder()
                .customerId(101)
                .name("Test Buyer")
                .email("buyer@hinchmart.com")
                .phone("9876543210")
                .build();

        order = Order.builder()
                .orderId(5001)
                .orderNumber("ORD-5001")
                .customer(customer)
                .totalAmount(BigDecimal.valueOf(1500.00))
                .paymentStatus("PENDING")
                .orderStatus("PLACED")
                .build();
    }

    @Test
    @DisplayName("createPaymentOrder - successfully creates Razorpay order for existing order")
    void testCreatePaymentOrder_Success() throws Exception {
        when(customerRepository.findById(101)).thenReturn(Optional.of(customer));
        when(orderRepository.findById(5001)).thenReturn(Optional.of(order));

        OrderClient mockOrdersClient = mock(OrderClient.class);
        ReflectionTestUtils.setField(razorpayClient, "orders", mockOrdersClient);

        com.razorpay.Order mockRzpOrder = mock(com.razorpay.Order.class);
        when(mockRzpOrder.get("id")).thenReturn("order_Mock12345");
        when(mockOrdersClient.create(any(JSONObject.class))).thenReturn(mockRzpOrder);

        Payment savedPayment = Payment.builder()
                .paymentId(1)
                .customer(customer)
                .order(order)
                .razorpayOrderId("order_Mock12345")
                .amount(BigDecimal.valueOf(1500.00))
                .status("CREATED")
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentOrderCreateRequest request = PaymentOrderCreateRequest.builder()
                .orderId(5001)
                .build();

        PaymentOrderCreateResponse response = paymentService.createPaymentOrder(101, request);

        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_Mock12345");
        assertThat(response.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(response.getAmountInPaise()).isEqualTo(150000L);
        assertThat(response.getKeyId()).isEqualTo("rzp_test_mockKey");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("createPaymentOrder - throws exception when amount is zero or negative")
    void testCreatePaymentOrder_InvalidAmount() {
        when(customerRepository.findById(101)).thenReturn(Optional.of(customer));

        PaymentOrderCreateRequest request = PaymentOrderCreateRequest.builder()
                .amount(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> paymentService.createPaymentOrder(101, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("verifyPayment - throws exception when cryptographic signature is invalid")
    void testVerifyPayment_InvalidSignature() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(5001)
                .razorpayOrderId("order_Mock12345")
                .razorpayPaymentId("pay_Mock98765")
                .razorpaySignature("invalid_tampered_signature_hash")
                .build();

        assertThatThrownBy(() -> paymentService.verifyPayment(101, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getLatestOrderPaymentStatus - returns stored payment details when paymentId not yet populated")
    void testGetLatestOrderPaymentStatus_CreatedState() {
        Payment payment = Payment.builder()
                .paymentId(10)
                .order(order)
                .customer(customer)
                .razorpayOrderId("order_Mock12345")
                .amount(BigDecimal.valueOf(1500.00))
                .currency("INR")
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(5001))
                .thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.getLatestOrderPaymentStatus(5001);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(5001);
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_Mock12345");
        assertThat(response.getStatus()).isEqualTo("CREATED");
        assertThat(response.getAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("createPaymentOrder - fetches amount directly from active Cart")
    void testCreatePaymentOrder_FromCart() throws Exception {
        when(customerRepository.findById(101)).thenReturn(Optional.of(customer));

        com.example.project.customer.dto.CartItemResponse item = com.example.project.customer.dto.CartItemResponse.builder()
                .productId(1)
                .quantity(2)
                .lineTotal(BigDecimal.valueOf(2500.00))
                .build();
        CartResponse mockCart = CartResponse.builder()
                .cartId(1)
                .items(java.util.List.of(item))
                .subtotal(BigDecimal.valueOf(2500.00))
                .grandTotal(BigDecimal.valueOf(2950.00))
                .build();
        when(cartService.getCart(101)).thenReturn(mockCart);

        OrderClient mockOrderClient = mock(OrderClient.class);
        ReflectionTestUtils.setField(razorpayClient, "orders", mockOrderClient);

        JSONObject rzpMockOrderJson = new JSONObject();
        rzpMockOrderJson.put("id", "order_CartOrder999");
        rzpMockOrderJson.put("amount", 295000L);
        rzpMockOrderJson.put("currency", "INR");

        com.razorpay.Order rzpOrder = new com.razorpay.Order(rzpMockOrderJson);
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(rzpOrder);

        PaymentOrderCreateRequest request = PaymentOrderCreateRequest.builder()
                .purpose("CART_PAYMENT")
                .build();

        PaymentOrderCreateResponse response = paymentService.createPaymentOrder(101, request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("2950.00");
        assertThat(response.getAmountInPaise()).isEqualTo(295000L);
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_CartOrder999");
        assertThat(response.getPurpose()).isEqualTo("CART_PAYMENT");
        verify(paymentRepository).save(any(Payment.class));
    }
}
