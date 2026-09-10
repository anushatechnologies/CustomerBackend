package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CouponResponse;
import com.example.project.customer.dto.SwitchStoreRequest;
import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.Cart;
import com.example.project.customer.entity.CartItem;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.StoreMismatchException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CartRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.StoreRepository;
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
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Integer userId) {
        Cart cart = getOrCreateActiveCart(userId);
        return calculateCartResponse(cart);
    }

    @Override
    public CartResponse addItem(Integer userId, CartItemRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Store productStore = resolveProductStore(product);
        if (productStore.getStatus() != StoreStatus.ACTIVE) {
            throw new IllegalStateException("Store '" + productStore.getName() + "' is currently not accepting new orders (status: " + productStore.getStatus() + ").");
        }

        Optional<Cart> activeCartOpt = cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(uid);
        Cart activeCart;

        if (activeCartOpt.isPresent()) {
            activeCart = activeCartOpt.get();
            List<CartItem> currentItems = cartItemRepository.findByCart_CartId(activeCart.getCartId());

            if (currentItems.isEmpty()) {
                // Empty cart adopted by the new item's store
                activeCart.setStore(productStore);
                cartRepository.save(activeCart);
            } else if (!activeCart.getStore().getStoreId().equals(productStore.getStoreId())) {
                // Different store -> Return 409 Store Mismatch Conflict
                log.warn("Customer #{} attempted adding item from Store #{} ('{}') to active cart locked to Store #{} ('{}')",
                        uid, productStore.getStoreId(), productStore.getName(),
                        activeCart.getStore().getStoreId(), activeCart.getStore().getName());
                throw new StoreMismatchException(activeCart.getStore(), productStore);
            }
        } else {
            // Activate or create cart for this product's store
            activeCart = getOrCreateCartForStore(uid, productStore);
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCart_CartIdAndProduct_ProductId(activeCart.getCartId(), product.getProductId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(activeCart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return calculateCartResponse(activeCart);
    }

    @Override
    public CartResponse switchStore(Integer userId, SwitchStoreRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        Store targetStore;

        if (request.getStoreId() != null) {
            targetStore = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target store not found with id: " + request.getStoreId()));
        } else if (request.getStoreSlug() != null && !request.getStoreSlug().isBlank()) {
            targetStore = storeRepository.findBySlugIgnoreCase(request.getStoreSlug().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Target store not found with slug: " + request.getStoreSlug()));
        } else {
            throw new IllegalArgumentException("Either storeId or storeSlug must be provided to switch active store.");
        }

        if (targetStore.getStatus() != StoreStatus.ACTIVE) {
            throw new IllegalStateException("Cannot switch to store '" + targetStore.getName() + "': Store is currently " + targetStore.getStatus());
        }

        // Deactivate all current active carts for this customer
        List<Cart> customerCarts = cartRepository.findByCustomer_CustomerId(uid);
        for (Cart c : customerCarts) {
            if (Boolean.TRUE.equals(c.getIsActive())) {
                c.setIsActive(false);
                cartRepository.save(c);
            }
        }

        // Find or create cart for target store
        Cart targetCart = getOrCreateCartForStore(uid, targetStore);
        targetCart.setIsActive(true);
        cartRepository.save(targetCart);

        // Optionally add the pending item that prompted the switch
        if (request.getPendingProductId() != null) {
            Product pendingProduct = productRepository.findById(request.getPendingProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pending product not found with id: " + request.getPendingProductId()));

            int qty = (request.getPendingQuantity() != null && request.getPendingQuantity() > 0) ? request.getPendingQuantity() : 1;
            Optional<CartItem> existing = cartItemRepository.findByCart_CartIdAndProduct_ProductId(targetCart.getCartId(), pendingProduct.getProductId());
            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(qty);
                cartItemRepository.save(item);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(targetCart)
                        .product(pendingProduct)
                        .quantity(qty)
                        .build();
                cartItemRepository.save(newItem);
            }
        }

        log.info("Customer #{} switched active cart to Store #{} ('{}')", uid, targetStore.getStoreId(), targetStore.getName());
        return calculateCartResponse(targetCart);
    }

    @Override
    public CartResponse removeItem(Integer userId, Integer productId) {
        Cart cart = getOrCreateActiveCart(userId);
        cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId)
                .ifPresent(cartItemRepository::delete);
        return calculateCartResponse(cart);
    }

    @Override
    public void clearCart(Integer userId) {
        Cart cart = getOrCreateActiveCart(userId);
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
    }

    @Override
    public CouponResponse applyCoupon(Integer userId, String couponCode) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        Cart cart = getOrCreateActiveCart(userId);
        String code = couponCode != null ? couponCode.trim().toUpperCase() : "";

        if (!"BUILDER50K".equalsIgnoreCase(code) && !"HINCH10".equalsIgnoreCase(code) && !"WELCOME500".equalsIgnoreCase(code)) {
            throw new IllegalArgumentException("Invalid coupon code: " + couponCode);
        }

        cart.setAppliedCoupon(code);
        cartRepository.save(cart);

        CartResponse response = calculateCartResponse(cart);

        return CouponResponse.builder()
                .couponCode(code)
                .discountAmount(response.getCouponDiscount())
                .newGrandTotal(response.getGrandTotal())
                .build();
    }

    public Cart getOrCreateActiveCart(Integer userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        return cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(uid)
                .orElseGet(() -> {
                    Store defaultStore = getDefaultStore();
                    return getOrCreateCartForStore(uid, defaultStore);
                });
    }

    private Cart getOrCreateCartForStore(Integer customerId, Store store) {
        // First deactivate any currently active cart
        cartRepository.findByCustomer_CustomerIdAndIsActiveTrue(customerId).ifPresent(active -> {
            active.setIsActive(false);
            cartRepository.save(active);
        });

        // Find or create cart for this store
        Cart cart = cartRepository.findByCustomer_CustomerIdAndStore_StoreId(customerId, store.getStoreId())
                .orElseGet(() -> Cart.builder()
                        .customer(Customer.builder().customerId(customerId).build())
                        .store(store)
                        .deliveryCharge(BigDecimal.valueOf(4500.0))
                        .isActive(true)
                        .build());

        cart.setIsActive(true);
        return cartRepository.save(cart);
    }

    private Store resolveProductStore(Product product) {
        if (product.getStore() != null) {
            return product.getStore();
        }
        return getDefaultStore();
    }

    private Store getDefaultStore() {
        return storeRepository.findById(1)
                .orElseGet(() -> storeRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No default marketplace store available.")));
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
        } else if ("WELCOME500".equalsIgnoreCase(cart.getAppliedCoupon())) {
            couponDiscount = BigDecimal.valueOf(500.0);
            if (couponDiscount.compareTo(subtotal) > 0) {
                couponDiscount = subtotal;
            }
        }

        BigDecimal deliveryCharge = (subtotal.compareTo(BigDecimal.valueOf(1000000)) >= 0 || items.isEmpty())
                ? BigDecimal.ZERO : BigDecimal.valueOf(4500.0);

        BigDecimal grandTotal = subtotal.subtract(couponDiscount).add(totalGst).add(deliveryCharge);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        Store store = cart.getStore() != null ? cart.getStore() : getDefaultStore();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .storeId(store.getStoreId())
                .storeName(store.getName())
                .storeSlug(store.getSlug())
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
