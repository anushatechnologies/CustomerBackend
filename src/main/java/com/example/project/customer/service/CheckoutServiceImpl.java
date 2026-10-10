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

        String originState = "Telangana";
        if (cart.getStoreId() != null) {
            Store store = storeRepository.findById(cart.getStoreId()).orElse(null);
            if (store != null && store.getSeller() != null && store.getSeller().getState() != null && !store.getSeller().getState().isBlank()) {
                originState = store.getSeller().getState().trim();
            }
        }

        String buyerState = address.getState();
        boolean isIntraState = taxCalculationService.isIntraState(originState, buyerState);

        BigDecimal freightCharge = (cart.getDeliveryCharge() != null
                && cart.getDeliveryCharge().compareTo(BigDecimal.valueOf(4500.0)) != 0)
                ? cart.getDeliveryCharge()
                : BigDecimal.ZERO;
        BigDecimal craneUnloadingCharge = Boolean.TRUE.equals(request.getRequiresCraneUnloading())
                ? BigDecimal.valueOf(2500.0) : BigDecimal.ZERO;

        List<TaxableItemInput> taxInputs = new ArrayList<>();
        if (cart.getItems() != null) {
            for (CartItemResponse item : cart.getItems()) {
                taxInputs.add(TaxableItemInput.builder()
                        .productId(item.getProductId())
                        .title(item.getTitle())
                        .hsnCode(item.getHsnCode())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .gstRate(item.getGstRate())
                        .build());
            }
        }

        TaxCalculationResult result = taxCalculationService.calculateTaxes(
                taxInputs,
                cart.getCouponDiscount(),
                isIntraState,
                freightCharge,
                craneUnloadingCharge
        );

        return CheckoutPreviewResponse.builder()
                .subtotal(result.getSubtotal())
                .discount(result.getDiscount())
                .taxableAmount(result.getTaxableAmount())
                .cgst(result.getCgst())
                .sgst(result.getSgst())
                .igst(result.getIgst())
                .totalGst(result.getTotalGst())
                .freightCharge(result.getFreightCharge())
                .craneUnloadingCharge(result.getCraneUnloadingCharge())
                .grandTotal(result.getGrandTotal())
                .build();
    }
}
