package com.example.project.customer.service;

import com.example.project.customer.dto.CartDto;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.entity.Address;
import com.example.project.customer.exception.BadRequestException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class CheckoutServiceImpl implements CheckoutService {

    private final CartService cartService;
    private final AddressRepository addressRepository;

    public CheckoutServiceImpl(CartService cartService, AddressRepository addressRepository) {
        this.cartService = cartService;
        this.addressRepository = addressRepository;
    }

    @Override
    public CheckoutPreviewResponse previewCheckout(Integer userId, CheckoutPreviewRequest request) {
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found with id: " + request.getAddressId()));

        CartDto cart = cartService.getCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot preview checkout for an empty cart");
        }

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal discount = cart.getCouponDiscount() != null ? cart.getCouponDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount);
        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        boolean isIntrastate = address.getState() != null &&
                address.getState().trim().equalsIgnoreCase("Telangana");

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal totalGst = taxableAmount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);

        if (isIntrastate) {
            cgst = taxableAmount.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
            sgst = taxableAmount.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        } else {
            igst = totalGst;
        }

        BigDecimal freightCharge = new BigDecimal("4500.00");
        BigDecimal craneUnloadingCharge = request.isRequiresCraneUnloading() ? new BigDecimal("2500.00") : BigDecimal.ZERO;

        BigDecimal grandTotal = taxableAmount
                .add(totalGst)
                .add(freightCharge)
                .add(craneUnloadingCharge);

        return new CheckoutPreviewResponse(
                subtotal,
                discount,
                taxableAmount,
                cgst,
                sgst,
                igst,
                totalGst,
                freightCharge,
                craneUnloadingCharge,
                grandTotal
        );
    }
}
