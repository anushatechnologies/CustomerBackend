package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.Cart;
import com.example.project.customer.entity.CartItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CartRepository;
import com.example.project.customer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        return calculateCartResponse(cart);
    }

    @Override
    public CartResponse addItem(Integer userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Optional<CartItem> existingItem = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), product.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return calculateCartResponse(cart);
    }

    @Override
    public CartResponse removeItem(Integer userId, Integer productId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId)
                .ifPresent(cartItemRepository::delete);
        return calculateCartResponse(cart);
    }

    @Override
    public void clearCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);
    }

    @Override
    public CouponResponse applyCoupon(Integer userId, String couponCode) {
        Cart cart = getOrCreateCart(userId);
        String code = couponCode != null ? couponCode.trim().toUpperCase() : "";

        BigDecimal discount;
        if ("BUILDER50K".equalsIgnoreCase(code)) {
            discount = BigDecimal.valueOf(50000.0);
            cart.setAppliedCoupon("BUILDER50K");
        } else if ("HINCH10".equalsIgnoreCase(code)) {
            cart.setAppliedCoupon("HINCH10");
            CartResponse temp = calculateCartResponse(cart);
            discount = temp.getSubtotal().multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            if (discount.compareTo(BigDecimal.valueOf(25000)) > 0) {
                discount = BigDecimal.valueOf(25000.0);
            }
        } else {
            throw new IllegalArgumentException("Invalid or expired coupon code: " + couponCode);
        }

        cartRepository.save(cart);
        CartResponse updated = calculateCartResponse(cart);

        return CouponResponse.builder()
                .couponCode(code)
                .discountAmount(discount)
                .newGrandTotal(updated.getGrandTotal())
                .build();
    }

    public Cart getOrCreateCart(Integer userId) {
        int uid = userId != null ? userId : 101;
        return cartRepository.findByUserId(uid)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(uid)
                            .deliveryCharge(BigDecimal.valueOf(4500.0))
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    public CartResponse calculateCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart_CartId(cart.getCartId());
        List<CartItemResponse> itemResponses = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product p = item.getProduct();
            int qty = item.getQuantity();

            BigDecimal originalPrice = p.getPrice();
            BigDecimal unitPrice = originalPrice;
            String appliedTier = null;

            // Real-time wholesale volume tier evaluation
            if (p.getBulkPricingTiers() != null && !p.getBulkPricingTiers().isEmpty()) {
                for (BulkPricingTier tier : p.getBulkPricingTiers()) {
                    boolean minMatch = tier.getMinQty() == null || qty >= tier.getMinQty();
                    boolean maxMatch = tier.getMaxQty() == null || qty <= tier.getMaxQty();
                    if (minMatch && maxMatch) {
                        unitPrice = tier.getPrice();
                        BigDecimal diff = originalPrice.subtract(unitPrice);
                        String maxQtyStr = tier.getMaxQty() != null ? String.valueOf(tier.getMaxQty()) : "+";
                        appliedTier = tier.getMinQty() + "-" + maxQtyStr + " " + p.getUnit() + " Tier (-₹" + diff.setScale(0, RoundingMode.HALF_UP) + "/" + p.getUnit() + ")";
                        break;
                    }
                }
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gstRate = p.getGstRate() != null ? p.getGstRate() : BigDecimal.valueOf(18.0);
            BigDecimal lineGst = lineTotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);
            totalGst = totalGst.add(lineGst);

            itemResponses.add(CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(p.getProductId())
                    .title(p.getTitle())
                    .imageUrl(p.getImageUrl())
                    .quantity(qty)
                    .unit(p.getUnit())
                    .unitPrice(unitPrice)
                    .originalPrice(originalPrice)
                    .appliedTier(appliedTier)
                    .gstRate(gstRate)
                    .lineTotal(lineTotal)
                    .lineGst(lineGst)
                    .build());
        }

        // Coupon calculation
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if ("BUILDER50K".equalsIgnoreCase(cart.getAppliedCoupon())) {
            couponDiscount = BigDecimal.valueOf(50000.0);
            if (couponDiscount.compareTo(subtotal) > 0) {
                couponDiscount = subtotal;
            }
        } else if ("HINCH10".equalsIgnoreCase(cart.getAppliedCoupon())) {
            couponDiscount = subtotal.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            if (couponDiscount.compareTo(BigDecimal.valueOf(25000)) > 0) {
                couponDiscount = BigDecimal.valueOf(25000.0);
            }
        }

        BigDecimal deliveryCharge = (subtotal.compareTo(BigDecimal.valueOf(1000000)) >= 0 || items.isEmpty())
                ? BigDecimal.ZERO : BigDecimal.valueOf(4500.0);

        BigDecimal grandTotal = subtotal.subtract(couponDiscount).add(totalGst).add(deliveryCharge);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(itemResponses)
                .subtotal(subtotal)
                .couponDiscount(couponDiscount)
                .totalGst(totalGst)
                .deliveryCharge(deliveryCharge)
                .grandTotal(grandTotal)
                .appliedCoupon(cart.getAppliedCoupon())
                .build();
    }
}
