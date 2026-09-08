package com.example.project.customer;

import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.service.CartService;
import com.example.project.customer.service.CheckoutService;
import com.example.project.customer.service.CheckoutServiceImpl;
import com.example.project.customer.service.OrderServiceImpl;
import com.example.project.customer.service.PdfInvoiceGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderAddressIntegrationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private PdfInvoiceGeneratorService pdfInvoiceGeneratorService;

    private OrderServiceImpl orderService;
    private CheckoutServiceImpl checkoutServiceImpl;

    private Customer customer101;
    private Address address1;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                productRepository,
                addressRepository,
                customerRepository,
                cartService,
                checkoutService,
                pdfInvoiceGeneratorService
        );

        checkoutServiceImpl = new CheckoutServiceImpl(cartService, addressRepository);

        customer101 = Customer.builder().customerId(101).name("Pavan Kumar").phone("9876543210").build();

        address1 = Address.builder()
                .id(10)
                .customer(customer101)
                .siteName("Project Cyber Tower")
                .recipientName("Pavan Kumar")
                .phone("9876543210")
                .houseFlatNo("Plot 42")
                .addressLine1("HITEC City Main Road")
                .areaLocality("Madhapur")
                .city("Hyderabad")
                .state("Telangana")
                .pincode("500081")
                .country("India")
                .isDefault(true)
                .build();
    }

    @Test
    @DisplayName("Order Checkout: Captures full delivery address snapshot on the order")
    void testOrderCreation_CapturesAddressSnapshot() {
        CartItemResponse item = CartItemResponse.builder()
                .productId(1)
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(500.0))
                .lineTotal(BigDecimal.valueOf(5000.0))
                .build();

        CartResponse cart = CartResponse.builder()
                .cartId(1)
                .items(List.of(item))
                .subtotal(BigDecimal.valueOf(5000.0))
                .build();

        when(cartService.getCart(101)).thenReturn(cart);
        when(addressRepository.findByCustomer_CustomerIdAndId(101, 10)).thenReturn(Optional.of(address1));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(BigDecimal.valueOf(5000.0))
                .discount(BigDecimal.ZERO)
                .taxableAmount(BigDecimal.valueOf(5000.0))
                .totalGst(BigDecimal.valueOf(900.0))
                .freightCharge(BigDecimal.valueOf(4500.0))
                .craneUnloadingCharge(BigDecimal.ZERO)
                .grandTotal(BigDecimal.valueOf(10400.0))
                .build();

        when(checkoutService.previewCheckout(eq(101), any(CheckoutPreviewRequest.class))).thenReturn(preview);

        Product product = Product.builder().productId(1).stockQty(100).build();
        when(productRepository.findByIdForStockUpdate(1)).thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(com.example.project.customer.entity.OrderItem.class)))
                .thenAnswer(invocation -> {
                    com.example.project.customer.entity.OrderItem oi = invocation.getArgument(0);
                    oi.setOrderItemId(1);
                    return oi;
                });

        OrderCreateRequest req = OrderCreateRequest.builder()
                .addressId(10)
                .paymentMethod("RAZORPAY")
                .build();

        OrderResponse orderResponse = orderService.createOrder(101, req);

        assertThat(orderResponse).isNotNull();

        // Verify that the saved Order entity contains the complete address snapshot
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, org.mockito.Mockito.atLeastOnce()).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getAllValues().get(0);
        assertThat(savedOrder.getDeliveryLocation()).contains("Project Cyber Tower");
        assertThat(savedOrder.getDeliveryLocation()).contains("Plot 42");
        assertThat(savedOrder.getDeliveryLocation()).contains("HITEC City Main Road");
        assertThat(savedOrder.getDeliveryLocation()).contains("Hyderabad");
        assertThat(savedOrder.getDeliveryLocation()).contains("500081");
        assertThat(savedOrder.getDeliveryLocation()).contains("Contact: Pavan Kumar / 9876543210");
        assertThat(savedOrder.getAddressId()).isEqualTo(10);
    }

    @Test
    @DisplayName("Order Checkout: Rejects order placement when address belongs to another customer")
    void testOrderCreation_RejectsUnauthorizedAddress() {
        CartItemResponse item = CartItemResponse.builder().productId(1).quantity(2).build();
        CartResponse cart = CartResponse.builder().items(List.of(item)).build();

        when(cartService.getCart(101)).thenReturn(cart);
        // Address 99 does NOT belong to customer 101
        when(addressRepository.findByCustomer_CustomerIdAndId(101, 99)).thenReturn(Optional.empty());

        OrderCreateRequest req = OrderCreateRequest.builder().addressId(99).build();

        assertThatThrownBy(() -> orderService.createOrder(101, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found with id: 99");
    }

    @Test
    @DisplayName("Checkout Preview: Rejects preview calculation when address belongs to another customer")
    void testCheckoutPreview_RejectsUnauthorizedAddress() {
        when(cartService.getCart(101)).thenReturn(CartResponse.builder().subtotal(BigDecimal.TEN).build());
        when(addressRepository.findByCustomer_CustomerIdAndId(101, 99)).thenReturn(Optional.empty());

        CheckoutPreviewRequest req = CheckoutPreviewRequest.builder().addressId(99).build();

        assertThatThrownBy(() -> checkoutServiceImpl.previewCheckout(101, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found with id: 99");
    }
}
