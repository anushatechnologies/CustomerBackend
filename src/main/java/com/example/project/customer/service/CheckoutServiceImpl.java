package com.example.project.customer.service;

import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.project.customer.entity.Store;
import com.example.project.customer.repository.StoreRepository;

import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.tax.TaxCalculationResult;
import com.example.project.customer.dto.tax.TaxableItemInput;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final StoreRepository storeRepository;
    private final TaxCalculationService taxCalculationService;

    public CheckoutServiceImpl(CartService cartService,
                               AddressRepository addressRepository,
                               StoreRepository storeRepository) {
        this(cartService, addressRepository, storeRepository, new TaxCalculationServiceImpl());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CheckoutServiceImpl(CartService cartService,
                               AddressRepository addressRepository,
                               StoreRepository storeRepository,
                               TaxCalculationService taxCalculationService) {
        this.cartService = cartService;
        this.addressRepository = addressRepository;
        this.storeRepository = storeRepository;
        this.taxCalculationService = taxCalculationService;
    }

    @Override
    public CheckoutPreviewResponse previewCheckout(Integer userId, CheckoutPreviewRequest request) {
        CartResponse cart = cartService.getCart(userId);
        Address address = addressRepository.findByCustomer_CustomerIdAndId(userId, request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        String buyerState = address.getState();
        BigDecimal freightCharge = (cart.getDeliveryCharge() != null
                && cart.getDeliveryCharge().compareTo(BigDecimal.valueOf(4500.0)) != 0)
                ? cart.getDeliveryCharge()
                : BigDecimal.ZERO;
        BigDecimal craneUnloadingCharge = Boolean.TRUE.equals(request.getRequiresCraneUnloading())
                ? BigDecimal.valueOf(2500.0) : BigDecimal.ZERO;

        List<CartItemResponse> cartItems = cart.getItems() != null ? cart.getItems() : new ArrayList<>();

        if (cartItems.isEmpty()) {
            return CheckoutPreviewResponse.builder()
                    .subtotal(BigDecimal.ZERO)
                    .discount(BigDecimal.ZERO)
                    .taxableAmount(BigDecimal.ZERO)
                    .cgst(BigDecimal.ZERO)
                    .sgst(BigDecimal.ZERO)
                    .igst(BigDecimal.ZERO)
                    .totalGst(BigDecimal.ZERO)
                    .freightCharge(freightCharge)
                    .craneUnloadingCharge(craneUnloadingCharge)
                    .grandTotal(freightCharge.add(craneUnloadingCharge))
                    .items(cartItems)
                    .storeGroups(new ArrayList<>())
                    .canPlaceOrder(false)
                    .orderBlockReason("Cannot place order with an empty cart.")
                    .build();
        }

        // Partition items by store
        java.util.Map<Integer, List<CartItemResponse>> storeItemsMap = new java.util.LinkedHashMap<>();
        for (CartItemResponse item : cartItems) {
            Integer sId = item.getStoreId() != null ? item.getStoreId() : (cart.getStoreId() != null ? cart.getStoreId() : 1);
            storeItemsMap.computeIfAbsent(sId, k -> new ArrayList<>()).add(item);
        }

        BigDecimal subtotalSum = BigDecimal.ZERO;
        BigDecimal taxableAmountSum = BigDecimal.ZERO;
        BigDecimal cgstSum = BigDecimal.ZERO;
        BigDecimal sgstSum = BigDecimal.ZERO;
        BigDecimal igstSum = BigDecimal.ZERO;
        BigDecimal totalGstSum = BigDecimal.ZERO;
        BigDecimal totalDiscount = cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal cartSubtotal = cart.getSubtotal() != null && cart.getSubtotal().compareTo(BigDecimal.ZERO) > 0
                ? cart.getSubtotal() : BigDecimal.ONE;

        List<com.example.project.customer.dto.CartStoreGroupResponse> previewStoreGroups = new ArrayList<>();
        BigDecimal allocatedDiscount = BigDecimal.ZERO;
        int storeIndex = 0;
        int totalStores = storeItemsMap.size();

        for (java.util.Map.Entry<Integer, List<CartItemResponse>> entry : storeItemsMap.entrySet()) {
            storeIndex++;
            Integer storeId = entry.getKey();
            List<CartItemResponse> sItems = entry.getValue();
            Store store = storeRepository.findById(storeId).orElse(null);

            String originState = "Telangana";
            if (store != null && store.getSeller() != null && store.getSeller().getState() != null && !store.getSeller().getState().isBlank()) {
                originState = store.getSeller().getState().trim();
            }

            boolean isIntraState = taxCalculationService.isIntraState(originState, buyerState);

            List<TaxableItemInput> sTaxInputs = new ArrayList<>();
            BigDecimal sSubtotal = BigDecimal.ZERO;
            for (CartItemResponse item : sItems) {
                sSubtotal = sSubtotal.add(item.getLineTotal());
                sTaxInputs.add(TaxableItemInput.builder()
                        .productId(item.getProductId())
                        .title(item.getTitle())
                        .hsnCode(item.getHsnCode())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .gstRate(item.getGstRate())
                        .build());
            }

            BigDecimal sDiscount = BigDecimal.ZERO;
            if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                if (storeIndex == totalStores) {
                    sDiscount = totalDiscount.subtract(allocatedDiscount);
                } else {
                    sDiscount = totalDiscount.multiply(sSubtotal).divide(cartSubtotal, 2, RoundingMode.HALF_UP);
                    allocatedDiscount = allocatedDiscount.add(sDiscount);
                }
            }

            BigDecimal sFreight = (storeIndex == 1) ? freightCharge : BigDecimal.ZERO;
            BigDecimal sCrane = (storeIndex == 1) ? craneUnloadingCharge : BigDecimal.ZERO;

            TaxCalculationResult sResult = taxCalculationService.calculateTaxes(
                    sTaxInputs,
                    sDiscount,
                    isIntraState,
                    sFreight,
                    sCrane
            );

            if (sResult.getSubtotal() != null) subtotalSum = subtotalSum.add(sResult.getSubtotal());
            if (sResult.getTaxableAmount() != null) taxableAmountSum = taxableAmountSum.add(sResult.getTaxableAmount());
            if (sResult.getCgst() != null) cgstSum = cgstSum.add(sResult.getCgst());
            if (sResult.getSgst() != null) sgstSum = sgstSum.add(sResult.getSgst());
            if (sResult.getIgst() != null) igstSum = igstSum.add(sResult.getIgst());
            if (sResult.getTotalGst() != null) totalGstSum = totalGstSum.add(sResult.getTotalGst());

            int sCount = sItems.stream().mapToInt(it -> it.getQuantity() != null ? it.getQuantity() : 0).sum();
            boolean sCanCheckout = sItems.stream().allMatch(ir ->
                    Boolean.TRUE.equals(ir.getInStock())
                    && ir.getQuantity() <= ir.getAvailableQty()
                    && !"CHECKOUT_DISABLED".equals(ir.getReason())
                    && !"UNAVAILABLE_NO_VENDOR_STOCK".equals(ir.getReason()));
            String sBlockReason = sCanCheckout ? null : "Items in this store are out of stock or unavailable.";

            previewStoreGroups.add(com.example.project.customer.dto.CartStoreGroupResponse.builder()
                    .storeId(storeId)
                    .storeName(store != null ? store.getName() : "Store #" + storeId)
                    .storeSlug(store != null ? store.getSlug() : null)
                    .items(sItems)
                    .subtotal(sSubtotal)
                    .discount(sDiscount)
                    .taxableAmount(sResult.getTaxableAmount())
                    .cgst(sResult.getCgst())
                    .sgst(sResult.getSgst())
                    .igst(sResult.getIgst())
                    .totalGst(sResult.getTotalGst())
                    .deliveryCharge(sFreight)
                    .craneUnloadingCharge(sCrane)
                    .grandTotal(sResult.getGrandTotal())
                    .itemCount(sCount)
                    .canCheckout(sCanCheckout)
                    .checkoutBlockReason(sBlockReason)
                    .build());
        }

        BigDecimal grandTotal = taxableAmountSum.add(totalGstSum).add(freightCharge).add(craneUnloadingCharge);

        boolean canPlaceOrder = true;
        String orderBlockReason = null;

        if (cartItems.isEmpty()) {
            canPlaceOrder = false;
            orderBlockReason = "Cannot place order with an empty cart.";
        } else {
            boolean hasUnavailable = cartItems.stream().anyMatch(ir ->
                    !Boolean.TRUE.equals(ir.getInStock())
                    || (ir.getAvailableQty() != null && ir.getQuantity() > ir.getAvailableQty())
                    || "CHECKOUT_DISABLED".equals(ir.getReason())
                    || "UNAVAILABLE_NO_VENDOR_STOCK".equals(ir.getReason()));
            if (hasUnavailable) {
                canPlaceOrder = false;
                orderBlockReason = "Order cannot be placed while any item is unavailable.";
            }
        }

        return CheckoutPreviewResponse.builder()
                .subtotal(subtotalSum)
                .discount(totalDiscount)
                .taxableAmount(taxableAmountSum)
                .cgst(cgstSum)
                .sgst(sgstSum)
                .igst(igstSum)
                .totalGst(totalGstSum)
                .freightCharge(freightCharge)
                .craneUnloadingCharge(craneUnloadingCharge)
                .grandTotal(grandTotal)
                .items(cartItems)
                .storeGroups(previewStoreGroups)
                .canPlaceOrder(canPlaceOrder)
                .orderBlockReason(orderBlockReason)
                .build();
    }
}
