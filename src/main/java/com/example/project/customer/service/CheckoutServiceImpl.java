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

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final StoreRepository storeRepository;

    @Override
    public CheckoutPreviewResponse previewCheckout(Integer userId, CheckoutPreviewRequest request) {
        CartResponse cart = cartService.getCart(userId);
        Address address = addressRepository.findByCustomer_CustomerIdAndId(userId, request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal discount = cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        // Pull store origin state from store.getSeller().getState() instead of hardcoded "Telangana"
        String originState = "Telangana";
        if (cart.getStoreId() != null) {
            Store store = storeRepository.findById(cart.getStoreId()).orElse(null);
            if (store != null && store.getSeller() != null && store.getSeller().getState() != null && !store.getSeller().getState().isBlank()) {
                originState = store.getSeller().getState().trim();
            }
        }

        String buyerState = address.getState() != null ? address.getState().trim() : "";
        boolean isIntraState = buyerState.isBlank() || originState.equalsIgnoreCase(buyerState);

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        if (isIntraState) {
            cgst = taxableAmount.multiply(BigDecimal.valueOf(0.09)).setScale(2, RoundingMode.HALF_UP);
            sgst = taxableAmount.multiply(BigDecimal.valueOf(0.09)).setScale(2, RoundingMode.HALF_UP);
        } else {
            igst = taxableAmount.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalGst = cgst.add(sgst).add(igst);
        BigDecimal freightCharge = cart.getDeliveryCharge() != null ? cart.getDeliveryCharge() : BigDecimal.valueOf(4500.0);
        BigDecimal craneUnloadingCharge = Boolean.TRUE.equals(request.getRequiresCraneUnloading())
                ? BigDecimal.valueOf(2500.0) : BigDecimal.ZERO;

        BigDecimal grandTotal = taxableAmount.add(totalGst).add(freightCharge).add(craneUnloadingCharge);

        return CheckoutPreviewResponse.builder()
                .subtotal(subtotal)
                .discount(discount)
                .taxableAmount(taxableAmount)
                .cgst(cgst)
                .sgst(sgst)
                .igst(igst)
                .totalGst(totalGst)
                .freightCharge(freightCharge)
                .craneUnloadingCharge(craneUnloadingCharge)
                .grandTotal(grandTotal)
                .build();
    }
}
