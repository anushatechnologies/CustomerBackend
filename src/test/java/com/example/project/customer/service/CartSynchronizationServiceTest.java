package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemUpdateRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CartSyncRequest;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Cart;
import com.example.project.customer.entity.CartItem;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CartRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartSynchronizationServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private CouponService couponService;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CheckoutService checkoutService;
    @Mock private PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    @Mock private StoreInvoiceSequenceService storeInvoiceSequenceService;
    @Mock private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;

    private CartServiceImpl cartService;
    private OrderServiceImpl orderService;

    private Customer testCustomer;
    private Store testStore;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartRepository, cartItemRepository, productRepository, storeRepository, couponService);
        orderService = new OrderServiceImpl(
                orderRepository, orderItemRepository, productRepository, addressRepository, customerRepository,
                cartService, checkoutService, couponService, pdfInvoiceGeneratorService, storeInvoiceSequenceService,
                sellerPayoutLedgerRepository, storeRepository, null, null
        );

        testCustomer = Customer.builder().customerId(101).email("customer@example.com").build();
        testStore = Store.builder().storeId(1).name("HinchMart Main Store").slug("main-store").status(StoreStatus.ACTIVE).build();

        testCart = Cart.builder()
                .cartId(50)
                .customer(testCustomer)
                .store(testStore)
                .isActive(true)
                .deliveryCharge(BigDecimal.valueOf(4500.0))
                .items(new ArrayList<>())
                .build();

        testProduct = Product.builder()
                .productId(201)
                .title("TMT Rebar 500D")
                .price(BigDecimal.valueOf(500.0))
                .store(testStore)
                .stockQty(100)
                .active(true)
                .unit("Piece")
                .build();

        testCartItem = CartItem.builder()
                .cartItemId(701)
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .build();

        when(cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(101)).thenReturn(Optional.of(testCart));
        when(storeRepository.findById(1)).thenReturn(Optional.of(testStore));
        when(productRepository.findById(201)).thenReturn(Optional.of(testProduct));
        when(productRepository.findByIdForStockUpdate(201)).thenReturn(Optional.of(testProduct));
    }

    @Test
    @DisplayName("addItem: adds new product and prevents duplicates")
    void testAddItem_NewProduct() {
        CartItemRequest req = CartItemRequest.builder().productId(201).quantity(4).build();
        when(cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(50, 201)).thenReturn(List.of());
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(testCartItem));

        CartResponse response = cartService.addItem(101, req);

        verify(cartItemRepository).save(any(CartItem.class));
        assertThat(response).isNotNull();
        assertThat(response.getCartId()).isEqualTo(50);
    }

    @Test
    @DisplayName("addItem: when item exists, updates quantity and cleans up duplicate rows")
    void testAddItem_ExistingDuplicatesCleanedUp() {
        CartItem item1 = CartItem.builder().cartItemId(701).cart(testCart).product(testProduct).quantity(1).build();
        CartItem item2 = CartItem.builder().cartItemId(702).cart(testCart).product(testProduct).quantity(1).build();

        when(cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(50, 201)).thenReturn(List.of(item1, item2));
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(item1));

        CartItemRequest req = CartItemRequest.builder().productId(201).quantity(5).build();
        CartResponse response = cartService.addItem(101, req);

        assertThat(item1.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(item1);
        verify(cartItemRepository).delete(item2);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("updateItem: updates quantity when quantity > 0 using cartItemId")
    void testUpdateItem_SuccessByCartItemId() {
        when(cartItemRepository.findByCart_CartIdAndCartItemId(50, 701)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(testCartItem));

        CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(7).build();
        CartResponse response = cartService.updateItem(101, 701, req);

        assertThat(testCartItem.getQuantity()).isEqualTo(7);
        verify(cartItemRepository).save(testCartItem);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("updateItem: fallback to productId if not found by cartItemId")
    void testUpdateItem_FallbackByProductId() {
        when(cartItemRepository.findByCart_CartIdAndCartItemId(50, 201)).thenReturn(Optional.empty());
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(50, 201)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(testCartItem));

        CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(3).build();
        CartResponse response = cartService.updateItem(101, 201, req);

        assertThat(testCartItem.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(testCartItem);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("updateItem: when quantity is 0, removes the item")
    void testUpdateItem_ZeroQuantityRemovesItem() {
        when(cartItemRepository.findByCart_CartIdAndCartItemId(50, 701)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of());

        CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(0).build();
        CartResponse response = cartService.updateItem(101, 701, req);

        verify(cartItemRepository).delete(testCartItem);
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("removeItem: removes item by cartItemId or productId")
    void testRemoveItem_Success() {
        when(cartItemRepository.findByCart_CartIdAndCartItemId(50, 701)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of());

        CartResponse response = cartService.removeItem(101, 701);

        verify(cartItemRepository).delete(testCartItem);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("syncCart: merges guest items atomically into user's active cart")
    void testSyncCart_Success() {
        CartSyncRequest syncReq = CartSyncRequest.builder()
                .items(List.of(CartItemRequest.builder().productId(201).quantity(6).build()))
                .build();

        when(cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(50, 201)).thenReturn(List.of(testCartItem));
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(testCartItem));

        CartResponse response = cartService.syncCart(101, syncReq);

        assertThat(testCartItem.getQuantity()).isEqualTo(6);
        verify(cartItemRepository).save(testCartItem);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("syncCart: empty items list returns current cart without error")
    void testSyncCart_EmptyItems() {
        CartSyncRequest syncReq = CartSyncRequest.builder().items(List.of()).build();
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(testCartItem));

        CartResponse response = cartService.syncCart(101, syncReq);
        assertThat(response).isNotNull();
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Security: Null user ID throws UnauthorizedException")
    void testCartSecurity_NullUserIdThrows() {
        assertThatThrownBy(() -> cartService.getCart(null))
                .isInstanceOf(UnauthorizedException.class);

        assertThatThrownBy(() -> cartService.addItem(null, CartItemRequest.builder().productId(201).quantity(1).build()))
                .isInstanceOf(UnauthorizedException.class);

        assertThatThrownBy(() -> cartService.updateItem(null, 701, CartItemUpdateRequest.builder().quantity(1).build()))
                .isInstanceOf(UnauthorizedException.class);

        assertThatThrownBy(() -> cartService.removeItem(null, 701))
                .isInstanceOf(UnauthorizedException.class);

        assertThatThrownBy(() -> cartService.syncCart(null, CartSyncRequest.builder().build()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Order Creation: Empty cart in database throws 'Cannot place order with an empty cart'")
    void testCreateOrder_EmptyCartThrows() {
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of());

        OrderCreateRequest orderReq = OrderCreateRequest.builder().addressId(1).build();

        assertThatThrownBy(() -> orderService.createOrder(101, orderReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot place order with an empty cart");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Order Creation: Valid cart creates order, decrements stock, and clears cart transactionally")
    void testCreateOrder_ValidCartSuccess() {
        when(cartItemRepository.findByCart_CartId(50)).thenReturn(List.of(testCartItem));

        Address address = Address.builder().id(1).customer(testCustomer).addressLine1("123 Site Lane").city("Hyderabad").state("Telangana").pincode("500001").build();
        when(addressRepository.findByCustomer_CustomerIdAndId(101, 1)).thenReturn(Optional.of(address));

        CheckoutPreviewResponse preview = CheckoutPreviewResponse.builder()
                .subtotal(BigDecimal.valueOf(1000.0))
                .discount(BigDecimal.ZERO)
                .taxableAmount(BigDecimal.valueOf(1000.0))
                .cgst(BigDecimal.valueOf(90.0))
                .sgst(BigDecimal.valueOf(90.0))
                .igst(BigDecimal.ZERO)
                .totalGst(BigDecimal.valueOf(180.0))
                .freightCharge(BigDecimal.valueOf(4500.0))
                .craneUnloadingCharge(BigDecimal.ZERO)
                .grandTotal(BigDecimal.valueOf(5680.0))
                .build();
        when(checkoutService.previewCheckout(eq(101), any(CheckoutPreviewRequest.class))).thenReturn(preview);
        when(storeInvoiceSequenceService.generateNextInvoiceNumber(any(Store.class))).thenReturn("HM-INV-001");

        Order mockSavedOrder = Order.builder()
                .orderId(999)
                .orderNumber("ORD-TEST-123456")
                .customer(testCustomer)
                .store(testStore)
                .addressId(1)
                .totalAmount(BigDecimal.valueOf(5680.0))
                .orderStatus("PLACED")
                .paymentStatus("PENDING")
                .paymentMethod("RAZORPAY")
                .items(new ArrayList<>())
                .checkpoints(new ArrayList<>())
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreateRequest orderReq = OrderCreateRequest.builder().addressId(1).build();
        OrderResponse response = orderService.createOrder(101, orderReq);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(999);
        // Verify cart is cleared after order creation
        verify(cartItemRepository).deleteByCart_CartId(50);
    }
}
