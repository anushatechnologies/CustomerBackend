package com.example.project.customer;

import com.example.project.customer.dto.*;
import com.example.project.customer.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CartAndCheckoutFlowTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Test
    void testCartBulkPricingEngineAndCheckout() {
        Integer userId = 101;

        // Find TMT Steel product (ID with 5-19, 20-49, 50+ MT slabs)
        var products = productService.getProducts(null, null, "TMT Steel", null, null, null, null, null, 1, 10);
        assertNotNull(products);
        assertFalse(products.getData().isEmpty());
        ProductResponse steel = products.getData().get(0);

        // Add 25 MT (should trigger Tier 2: ₹52,800/MT vs base ₹54,200/MT)
        CartDto cart = cartService.addItem(userId, new AddToCartRequest(steel.getProductId(), 25));
        assertNotNull(cart);
        assertFalse(cart.getItems().isEmpty());

        CartItemDto item = cart.getItems().get(0);
        assertEquals(25, item.getQuantity());
        assertEquals(new BigDecimal("52800.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("1320000.00"), item.getLineTotal());
        assertNotNull(item.getAppliedTier());

        // Apply coupon BUILDER50K
        ApplyCouponResponse couponResp = cartService.applyCoupon(userId, "BUILDER50K");
        assertNotNull(couponResp);
        assertEquals("BUILDER50K", couponResp.getCouponCode());
        assertEquals(new BigDecimal("50000.00"), couponResp.getDiscountAmount());

        // Get address
        List<AddressResponse> addresses = addressService.getAddresses(userId);
        assertNotNull(addresses);
        assertFalse(addresses.isEmpty());
        AddressResponse addr = addresses.get(0);

        // Preview checkout
        CheckoutPreviewResponse preview = checkoutService.previewCheckout(userId,
                new CheckoutPreviewRequest(addr.getAddressId(), "TOMORROW_MORNING", true));
        assertNotNull(preview);
        assertEquals(new BigDecimal("1320000.00"), preview.getSubtotal());
        assertEquals(new BigDecimal("50000.00"), preview.getDiscount());
        assertEquals(new BigDecimal("1270000.00"), preview.getTaxableAmount());
        assertEquals(new BigDecimal("228600.00"), preview.getTotalGst());
        assertEquals(new BigDecimal("4500.00"), preview.getFreightCharge());
        assertEquals(new BigDecimal("2500.00"), preview.getCraneUnloadingCharge());
        assertEquals(new BigDecimal("1505600.00"), preview.getGrandTotal());

        // Place Order
        CreateOrderRequest orderReq = new CreateOrderRequest(
                addr.getAddressId(),
                "RAZORPAY",
                "TOMORROW_MORNING",
                "Unload at Tower B Basement.",
                "PO-2026-HYD-0044",
                true
        );
        OrderResponse orderResp = orderService.placeOrder(userId, orderReq);
        assertNotNull(orderResp);
        assertNotNull(orderResp.getOrderNumber());
        assertEquals(new BigDecimal("1505600.00"), orderResp.getTotalAmount());
        assertEquals("PLACED", orderResp.getOrderStatus());

        // Verify Live Tracking
        OrderTrackingResponse tracking = orderService.getOrderTracking(userId, orderResp.getOrderId());
        assertNotNull(tracking);
        assertNotNull(tracking.getCheckpoints());
        assertFalse(tracking.getCheckpoints().isEmpty());

        // Verify B2B GST Tax Invoice
        InvoiceResponse invoice = orderService.getOrderInvoice(userId, orderResp.getOrderId());
        assertNotNull(invoice);
        assertNotNull(invoice.getInvoiceNumber());
        assertEquals("36AAACH2026Q1Z1", invoice.getSellerGstin());
        assertEquals("36AAACT2727Q1ZW", invoice.getBuyerGstin());
        assertEquals(new BigDecimal("1270000.00"), invoice.getTaxableAmount());
        assertEquals(new BigDecimal("1505600.00"), invoice.getGrandTotal());
    }
}
