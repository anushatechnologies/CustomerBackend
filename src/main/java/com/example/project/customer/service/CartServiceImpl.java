package com.example.project.customer.service;

import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.BadRequestException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CartRepository;
import com.example.project.customer.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    public CartDto getCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        return calculateCart(cart);
    }

    @Override
    public CartDto addItem(Integer userId, AddToCartRequest request) {
        if (request.getProductId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Invalid product or quantity");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Cart cart = getOrCreateCart(userId);
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(product.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(request.getQuantity());
        } else {
            CartItem newItem = new CartItem(product, request.getQuantity());
            cart.addItem(newItem);
        }

        Cart saved = cartRepository.save(cart);
        return calculateCart(saved);
    }

    @Override
    public CartDto removeItem(Integer userId, Integer cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        cart.removeItem(item);
        Cart saved = cartRepository.save(cart);
        return calculateCart(saved);
    }

    @Override
    public CartDto clearCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cart.setCouponDiscount(BigDecimal.ZERO);
        Cart saved = cartRepository.save(cart);
        return calculateCart(saved);
    }

    @Override
    public ApplyCouponResponse applyCoupon(Integer userId, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new BadRequestException("Coupon code cannot be empty");
        }

        Cart cart = getOrCreateCart(userId);
        String code = couponCode.trim().toUpperCase();

        BigDecimal discountAmount = BigDecimal.ZERO;
        if ("BUILDER50K".equals(code)) {
            discountAmount = new BigDecimal("50000.00");
        } else if ("HINCH10".equals(code)) {
            CartDto current = calculateCart(cart);
            discountAmount = current.getSubtotal().multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        } else if ("EXPRESS5K".equals(code)) {
            discountAmount = new BigDecimal("5000.00");
        } else {
            throw new BadRequestException("Invalid or expired coupon code: " + code);
        }

        cart.setAppliedCoupon(code);
        cart.setCouponDiscount(discountAmount);
        Cart saved = cartRepository.save(cart);

        CartDto recalculated = calculateCart(saved);
        return new ApplyCouponResponse(code, discountAmount, recalculated.getGrandTotal());
    }

    public Cart getOrCreateCart(Integer userId) {
        Integer uid = userId != null ? userId : 101;
        return cartRepository.findByUserId(uid).orElseGet(() -> {
            Cart newCart = new Cart(uid);
            return cartRepository.save(newCart);
        });
    }

    public CartDto calculateCart(Cart cart) {
        List<CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            int qty = item.getQuantity();
            BigDecimal basePrice = p.getPrice();
            BigDecimal effectivePrice = basePrice;
            String appliedTier = null;

            if (p.getBulkPricingTiers() != null && !p.getBulkPricingTiers().isEmpty()) {
                for (BulkPricingTier tier : p.getBulkPricingTiers()) {
                    boolean minMatch = qty >= tier.getMinQty();
                    boolean maxMatch = tier.getMaxQty() == null || qty <= tier.getMaxQty();
                    if (minMatch && maxMatch) {
                        effectivePrice = tier.getPrice();
                        BigDecimal diff = basePrice.subtract(effectivePrice);
                        if (diff.compareTo(BigDecimal.ZERO) > 0) {
                            String range = tier.getMaxQty() != null ?
                                    tier.getMinQty() + "-" + tier.getMaxQty() + " " + p.getUnit() :
                                    tier.getMinQty() + "+ " + p.getUnit();
                            appliedTier = range + " Tier (-₹" + diff.setScale(0, RoundingMode.HALF_UP) + "/" + p.getUnit() + ")";
                        }
                        break;
                    }
                }
            }

            BigDecimal lineTotal = effectivePrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            double gstRate = p.getGstRate() != null ? p.getGstRate() : 18.0;
            BigDecimal lineGst = lineTotal.multiply(BigDecimal.valueOf(gstRate / 100.0)).setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);
            totalGst = totalGst.add(lineGst);

            CartItemDto itemDto = new CartItemDto(
                    item.getCartItemId(),
                    p.getProductId(),
                    p.getTitle(),
                    p.getImageUrl(),
                    qty,
                    p.getUnit(),
                    effectivePrice,
                    basePrice,
                    appliedTier,
                    gstRate,
                    lineTotal,
                    lineGst
            );
            itemDtos.add(itemDto);
        }

        BigDecimal couponDiscount = cart.getCouponDiscount();
        if (couponDiscount == null) {
            couponDiscount = BigDecimal.ZERO;
        }
        if (couponDiscount.compareTo(subtotal) > 0) {
            couponDiscount = subtotal;
        }

        BigDecimal taxableAmount = subtotal.subtract(couponDiscount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        // Recalculate total GST based on taxable amount
        if (couponDiscount.compareTo(BigDecimal.ZERO) > 0 && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            totalGst = taxableAmount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal deliveryCharge = itemDtos.isEmpty() ? BigDecimal.ZERO : new BigDecimal("4500.00");
        BigDecimal grandTotal = taxableAmount.add(totalGst).add(deliveryCharge);

        return new CartDto(
                cart.getCartId(),
                itemDtos,
                subtotal,
                couponDiscount,
                totalGst,
                deliveryCharge,
                grandTotal,
                cart.getAppliedCoupon()
        );
    }
}
