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

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final AddressRepository addressRepository;

    @Override
    public CheckoutPreviewResponse previewCheckout(Integer userId, CheckoutPreviewRequest request) {
        CartResponse cart = cartService.getCart(userId);
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal discount = cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        // Split GST determination based on destination state (e.g. Telangana / intra-state vs interstate)
        boolean isIntraState = address.getState() == null || "Telangana".equalsIgnoreCase(address.getState().trim());

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
