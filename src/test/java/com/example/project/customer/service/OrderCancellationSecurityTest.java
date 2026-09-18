package com.example.project.customer.service;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Role;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.FirebaseUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationSecurityTest {

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
    private CouponService couponService;
    @Mock
    private PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    @Mock
    private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock
    private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserContextUtil userContextUtil;
    @Mock
    private SellerContextUtil sellerContextUtil;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderServiceImpl orderService;

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
                couponService,
                pdfInvoiceGeneratorService,
                storeInvoiceSequenceService,
                sellerPayoutLedgerRepository,
                storeRepository,
                userContextUtil,
                sellerContextUtil
        );
        orderService.setApplicationEventPublisher(eventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(int userId, String email, Role role, Integer sellerId) {
        FirebaseUserPrincipal principal = FirebaseUserPrincipal.builder()
                .firebaseUid("uid-" + userId)
                .internalUserId(userId)
                .email(email)
                .role(role)
                .sellerId(sellerId)
                .authorities(List.of(new SimpleGrantedAuthority(role.getAuthority())))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Order createSampleOrder(int orderId, int customerId, int sellerId) {
        Customer customer = Customer.builder()
                .customerId(customerId)
                .email("cust" + customerId + "@example.com")
                .name("Customer " + customerId)
                .build();

        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setName("Seller " + sellerId);

        Store store = new Store();
        store.setStoreId(sellerId * 10);
        store.setSeller(seller);

        return Order.builder()
                .orderId(orderId)
                .orderNumber("ORD-" + orderId)
                .customer(customer)
                .store(store)
                .orderStatus("CONFIRMED")
                .paymentStatus("PAID")
                .totalAmount(new BigDecimal("500.00"))
                .items(new ArrayList<>())
                .checkpoints(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Security: Customer successfully cancels their own order")
    void cancelOrder_CustomerOwnOrder_Success() {
        int customerId = 10;
        int orderId = 101;
        authenticateUser(customerId, "cust10@example.com", Role.CUSTOMER, null);
        when(userContextUtil.getOptionalCurrentUserId()).thenReturn(customerId);

        Order order = createSampleOrder(orderId, customerId, 5);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, "Home", "No longer needed");

        assertNotNull(response);
        assertEquals("CANCELLED", response.getOrderStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Security: Customer CANNOT cancel another customer's order (IDOR attack rejected with 403)")
    void cancelOrder_CustomerAnotherOrder_Forbidden() {
        int attackerCustomerId = 10;
        int victimCustomerId = 20;
        int targetOrderId = 202;

        authenticateUser(attackerCustomerId, "cust10@example.com", Role.CUSTOMER, null);
        when(userContextUtil.getOptionalCurrentUserId()).thenReturn(attackerCustomerId);

        Order victimOrder = createSampleOrder(targetOrderId, victimCustomerId, 5);
        when(orderRepository.findById(targetOrderId)).thenReturn(Optional.of(victimOrder));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                orderService.cancelOrder(targetOrderId, "Fake Location", "Malicious cancel")
        );

        assertEquals("Access denied: You can only cancel your own orders.", ex.getMessage());
    }

    @Test
    @DisplayName("Security: Unauthenticated request to cancel order is rejected with 403 Forbidden")
    void cancelOrder_Unauthenticated_Rejected() {
        int orderId = 303;
        SecurityContextHolder.clearContext();
        when(userContextUtil.getOptionalCurrentUserId()).thenReturn(null);

        Order order = createSampleOrder(orderId, 15, 5);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class, () ->
                orderService.cancelOrder(orderId, null, null)
        );
    }

    @Test
    @DisplayName("Security: Non-existent order ID throws ResourceNotFoundException (404)")
    void cancelOrder_NonExistentId_NotFound() {
        when(orderRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelOrder(9999, null, null)
        );
    }

    @Test
    @DisplayName("Security: Admin can cancel any customer order")
    void cancelOrder_Admin_Allowed() {
        int adminId = 1;
        int orderId = 404;
        authenticateUser(adminId, "admin@hinchmart.com", Role.ADMIN, null);

        Order order = createSampleOrder(orderId, 88, 5);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, "Admin Panel", "Cancelled by Admin");

        assertNotNull(response);
        assertEquals("CANCELLED", response.getOrderStatus());
    }

    @Test
    @DisplayName("Security: Seller can cancel an order placed in their own store")
    void cancelOrder_SellerOwnStore_Allowed() {
        int sellerId = 5;
        int orderId = 505;
        authenticateUser(50, "seller5@stores.com", Role.SELLER, sellerId);

        Order order = createSampleOrder(orderId, 99, sellerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, "Warehouse", "Out of stock item");

        assertNotNull(response);
        assertEquals("CANCELLED", response.getOrderStatus());
    }

    @Test
    @DisplayName("Security: Seller CANNOT cancel an order placed in another seller's store")
    void cancelOrder_SellerOtherStore_Forbidden() {
        int sellerId = 5;
        int otherSellerId = 8;
        int orderId = 606;
        authenticateUser(50, "seller5@stores.com", Role.SELLER, sellerId);

        Order otherStoreOrder = createSampleOrder(orderId, 99, otherSellerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(otherStoreOrder));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                orderService.cancelOrder(orderId, "Warehouse", "Not my store")
        );

        assertEquals("Access denied: You can only cancel your own orders.", ex.getMessage());
    }
}
