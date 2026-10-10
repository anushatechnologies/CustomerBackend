package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartItemUpdateRequest;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CartSyncRequest;
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

import com.example.project.customer.dto.tax.TaxCalculationResult;
import com.example.project.customer.dto.tax.TaxableItemInput;
import com.example.project.customer.dto.tax.TaxableItemResult;

@Slf4j
@Service
@Transactional
@SuppressWarnings("null")
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CouponService couponService;
    private final TaxCalculationService taxCalculationService;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           StoreRepository storeRepository,
                           CouponService couponService) {
        this(cartRepository, cartItemRepository, productRepository, storeRepository, couponService, new TaxCalculationServiceImpl());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           StoreRepository storeRepository,
                           CouponService couponService,
                           TaxCalculationService taxCalculationService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.couponService = couponService;
        this.taxCalculationService = taxCalculationService;
    }

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

        List<CartItem> existingItems = cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(activeCart.getCartId(), product.getProductId());
        if (!existingItems.isEmpty()) {
            CartItem item = existingItems.get(0);
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
            for (int i = 1; i < existingItems.size(); i++) {
                CartItem extra = existingItems.get(i);
                if (activeCart.getItems() != null) {
                    activeCart.getItems().remove(extra);
                }
                cartItemRepository.delete(extra);
            }
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(activeCart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        revalidateAppliedCoupon(activeCart);
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
        revalidateAppliedCoupon(targetCart);
        return calculateCartResponse(targetCart);
    }

    @Override
    public CartResponse updateItem(Integer userId, Integer id, CartItemUpdateRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        if (id == null) {
            throw new IllegalArgumentException("Cart item ID must not be null.");
        }
        Cart cart = getOrCreateActiveCart(userId);

        // Prefer cartItemId as primary identifier; fallback to productId for backward compatibility
        CartItem item = cartItemRepository.findByCart_CartIdAndCartItemId(cart.getCartId(), id)
                .orElseGet(() -> cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), id)
                        .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + id)));

        if (request.getQuantity() == null || request.getQuantity() == 0) {
            if (cart.getItems() != null) {
                cart.getItems().remove(item);
            }
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        revalidateAppliedCoupon(cart);
        return calculateCartResponse(cart);
    }

    @Override
    public CartResponse removeItem(Integer userId, Integer id) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        if (id == null) {
            throw new IllegalArgumentException("Cart item ID must not be null.");
        }
        Cart cart = getOrCreateActiveCart(userId);
        // Prefer cartItemId as primary identifier; fallback to productId for backward compatibility
        Optional<CartItem> itemOpt = cartItemRepository.findByCart_CartIdAndCartItemId(cart.getCartId(), id);
        if (itemOpt.isEmpty()) {
            itemOpt = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), id);
        }
        itemOpt.ifPresent(item -> {
            if (cart.getItems() != null) {
                cart.getItems().remove(item);
            }
            cartItemRepository.delete(item);
        });
        revalidateAppliedCoupon(cart);
        return calculateCartResponse(cart);
    }

    @Override
    public CartResponse syncCart(Integer userId, CartSyncRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return getCart(uid);
        }

        Cart activeCart = getOrCreateActiveCart(uid);

        // Resolve store if targetStoreId provided, or infer from incoming items if not provided
        if (request.getTargetStoreId() != null) {
            Store targetStore = storeRepository.findById(request.getTargetStoreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target store not found with id: " + request.getTargetStoreId()));
            if (targetStore.getStatus() != StoreStatus.ACTIVE) {
                throw new IllegalStateException("Cannot sync cart to store '" + targetStore.getName() + "': Store is currently " + targetStore.getStatus());
            }
            if (!activeCart.getStore().getStoreId().equals(targetStore.getStoreId())) {
                activeCart = getOrCreateCartForStore(uid, targetStore);
                activeCart.setIsActive(true);
                cartRepository.save(activeCart);
            }
        } else if (!request.getItems().isEmpty()) {
            for (CartItemRequest itemReq : request.getItems()) {
                if (itemReq.getProductId() != null) {
                    Product product = productRepository.findById(itemReq.getProductId()).orElse(null);
                    if (product != null && product.isActive()) {
                        Store productStore = resolveProductStore(product);
                        if (productStore.getStatus() == StoreStatus.ACTIVE
                                && activeCart.getStore() != null
                                && !activeCart.getStore().getStoreId().equals(productStore.getStoreId())) {
                            activeCart = getOrCreateCartForStore(uid, productStore);
                            break;
                        }
                    }
                }
            }
        }

        for (CartItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                continue;
            }
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (!product.isActive()) {
                continue;
            }

            Store productStore = resolveProductStore(product);
            if (productStore.getStatus() != StoreStatus.ACTIVE) {
                continue;
            }

            List<CartItem> currentItems = cartItemRepository.findByCart_CartId(activeCart.getCartId());
            if (currentItems.isEmpty()) {
                activeCart.setStore(productStore);
                activeCart = cartRepository.save(activeCart);
            } else if (!activeCart.getStore().getStoreId().equals(productStore.getStoreId())) {
                log.warn("Skipping product #{} during cart sync due to store mismatch with active cart (store #{} vs #{})",
                        product.getProductId(), activeCart.getStore().getStoreId(), productStore.getStoreId());
                continue;
            }

            List<CartItem> existingItems = cartItemRepository.findAllByCart_CartIdAndProduct_ProductId(activeCart.getCartId(), product.getProductId());
            if (!existingItems.isEmpty()) {
                CartItem item = existingItems.get(0);
                item.setQuantity(itemReq.getQuantity());
                cartItemRepository.save(item);
                for (int i = 1; i < existingItems.size(); i++) {
                    CartItem extra = existingItems.get(i);
                    if (activeCart.getItems() != null) {
                        activeCart.getItems().remove(extra);
                    }
                    cartItemRepository.delete(extra);
                }
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(activeCart)
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .build();
                cartItemRepository.save(newItem);
            }
        }

        revalidateAppliedCoupon(activeCart);
        return calculateCartResponse(activeCart);
    }

    @Override
    public void clearCart(Integer userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        Cart cart = getOrCreateActiveCart(userId);
        if (cart.getItems() != null) {
            cart.getItems().clear();
        }
        cart.setAppliedCoupon(null);
        cart.setDeliveryCharge(BigDecimal.ZERO);
        cartRepository.save(cart);
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
    }

    @Override
    public CouponResponse applyCoupon(Integer userId, String couponCode) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        Cart cart = getOrCreateActiveCart(userId);
        String code = couponCode != null ? couponCode.trim() : "";
        CartResponse beforeCoupon = calculateCartResponse(cart, false);
        var validation = couponService.validateAndCalculateDiscount(code, cart.getCustomer(), beforeCoupon.getSubtotal());

        cart.setAppliedCoupon(validation.getCoupon().getCode());
        cartRepository.save(cart);

        CartResponse response = calculateCartResponse(cart);

        return CouponResponse.builder()
                .couponCode(validation.getCoupon().getCode())
                .discountAmount(response.getCouponDiscount())
                .newGrandTotal(response.getGrandTotal())
                .cartSubtotal(response.getSubtotal())
                .taxAmount(response.getTotalGst())
                .deliveryCharge(response.getDeliveryCharge())
                .grandTotal(response.getGrandTotal())
                .build();
    }

    @Override
    public CartResponse removeCoupon(Integer userId) {
        Cart cart = getOrCreateActiveCart(userId);
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);
        revalidateAppliedCoupon(cart);
        return calculateCartResponse(cart);
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
                        .deliveryCharge(BigDecimal.ZERO)
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
        return storeRepository.findAll().stream()
                .filter(s -> s.getStatus() == StoreStatus.ACTIVE)
                .findFirst()
                .or(() -> storeRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("No default marketplace store available."));
    }

    public CartResponse calculateCartResponse(Cart cart) {
        return calculateCartResponse(cart, true);
    }

    private void revalidateAppliedCoupon(Cart cart) {
        if (cart.getAppliedCoupon() == null || cart.getAppliedCoupon().isBlank()) {
            return;
        }
        try {
            BigDecimal subtotal = calculateCartResponse(cart, false).getSubtotal();
            couponService.validateAndCalculateDiscount(cart.getAppliedCoupon(), cart.getCustomer(), subtotal);
        } catch (RuntimeException exception) {
            log.info("Removing ineligible coupon {} from cart {}: {}", cart.getAppliedCoupon(), cart.getCartId(), exception.getMessage());
            cart.setAppliedCoupon(null);
            cartRepository.save(cart);
        }
    }

    private CartResponse calculateCartResponse(Cart cart, boolean includeCoupon) {
        List<CartItem> items = cartItemRepository.findByCart_CartId(cart.getCartId());
        List<CartItemResponse> itemResponses = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;

        List<TaxableItemInput> taxInputs = new ArrayList<>();
        List<CartItemResponse.CartItemResponseBuilder> responseBuilders = new ArrayList<>();

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
            BigDecimal gstRate = p.getGstRate();
            if (gstRate == null || gstRate.compareTo(BigDecimal.ZERO) < 0 || gstRate.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalStateException("Product '" + p.getTitle() + "' (ID: " + p.getProductId()
                        + ") has missing or invalid GST rate: " + gstRate);
            }

            subtotal = subtotal.add(lineTotal);

            taxInputs.add(TaxableItemInput.builder()
                    .productId(p.getProductId())
                    .title(p.getTitle())
                    .hsnCode(p.getHsnCode())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .gstRate(gstRate)
                    .build());

            responseBuilders.add(CartItemResponse.builder()
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
                    .hsnCode(p.getHsnCode()));
        }

        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (includeCoupon && cart.getAppliedCoupon() != null && !cart.getAppliedCoupon().isBlank()) {
            try {
                couponDiscount = couponService
                        .validateAndCalculateDiscount(cart.getAppliedCoupon(), cart.getCustomer(), subtotal)
                        .getDiscount();
            } catch (RuntimeException exception) {
                // A coupon can become ineligible after it was applied (expiry, limits, etc.).
                // Keep the cart readable; checkout revalidates it before recording usage.
                log.info("Applied coupon {} is no longer eligible for cart {}: {}",
                        cart.getAppliedCoupon(), cart.getCartId(), exception.getMessage());
            }
        }

        BigDecimal deliveryCharge = (cart.getDeliveryCharge() != null
                && cart.getDeliveryCharge().compareTo(BigDecimal.valueOf(4500.0)) != 0)
                ? cart.getDeliveryCharge()
                : BigDecimal.ZERO;

        TaxCalculationResult taxResult = taxCalculationService.calculateTaxes(
                taxInputs,
                couponDiscount,
                true,
                deliveryCharge,
                BigDecimal.ZERO
        );

        for (int i = 0; i < responseBuilders.size(); i++) {
            TaxableItemResult tr = taxResult.getItemResults().get(i);
            responseBuilders.get(i).lineGst(tr.getLineGst());
            itemResponses.add(responseBuilders.get(i).build());
        }

        BigDecimal grandTotal = taxResult.getGrandTotal();
        BigDecimal totalGst = taxResult.getTotalGst();

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
