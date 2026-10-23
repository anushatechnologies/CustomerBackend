package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.dto.CartResponse;
import com.example.project.customer.dto.CartStoreGroupResponse;
import com.example.project.customer.dto.CheckoutPreviewRequest;
import com.example.project.customer.dto.CheckoutPreviewResponse;
import com.example.project.customer.dto.InvoiceResponse;
import com.example.project.customer.dto.OrderCreateRequest;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.OrderSummaryResponse;
import com.example.project.customer.dto.OrderTrackingResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.SellerOrderDto;
import com.example.project.customer.dto.SellerOrderPageData;
import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderGroup;
import com.example.project.customer.entity.OrderInventoryState;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.entity.Store;
import com.example.project.customer.repository.OrderGroupRepository;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.config.PlatformMarketplaceProperties;
import com.example.project.customer.dto.settlement.ItemSettlementSnapshot;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.config.UserContextUtil;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional

@SuppressWarnings("null")
public class OrderServiceImpl implements OrderService, org.springframework.context.ApplicationEventPublisherAware {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final CouponService couponService;
    private final PdfInvoiceGeneratorService pdfInvoiceGeneratorService;
    private final StoreInvoiceSequenceService storeInvoiceSequenceService;
    private final SellerPayoutLedgerRepository sellerPayoutLedgerRepository;
    private final StoreRepository storeRepository;
    private final UserContextUtil userContextUtil;
    private final SellerContextUtil sellerContextUtil;
    private SettlementCalculationService settlementCalculationService;
    private PlatformMarketplaceProperties platformMarketplaceProperties;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSettlementCalculationService(SettlementCalculationService settlementCalculationService) {
        this.settlementCalculationService = settlementCalculationService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPlatformMarketplaceProperties(PlatformMarketplaceProperties platformMarketplaceProperties) {
        this.platformMarketplaceProperties = platformMarketplaceProperties;
    }

    private VendorSelectionService vendorSelectionService;
    private com.example.project.customer.repository.HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setVendorSelectionService(VendorSelectionService vendorSelectionService) {
        this.vendorSelectionService = vendorSelectionService;
    }

    private ProductAvailabilityService productAvailabilityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setProductAvailabilityService(ProductAvailabilityService productAvailabilityService) {
        this.productAvailabilityService = productAvailabilityService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setHinchStoreVendorInventoryRepository(com.example.project.customer.repository.HinchStoreVendorInventoryRepository hinchStoreVendorInventoryRepository) {
        this.hinchStoreVendorInventoryRepository = hinchStoreVendorInventoryRepository;
    }

    private OrderGroupRepository orderGroupRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setOrderGroupRepository(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            AddressRepository addressRepository,
            CustomerRepository customerRepository,
            CartService cartService,
            CheckoutService checkoutService,
            CouponService couponService,
            PdfInvoiceGeneratorService pdfInvoiceGeneratorService,
            StoreInvoiceSequenceService storeInvoiceSequenceService,
            SellerPayoutLedgerRepository sellerPayoutLedgerRepository,
            StoreRepository storeRepository,
            UserContextUtil userContextUtil,
            SellerContextUtil sellerContextUtil
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.couponService = couponService;
        this.pdfInvoiceGeneratorService = pdfInvoiceGeneratorService;
        this.storeInvoiceSequenceService = storeInvoiceSequenceService;
        this.sellerPayoutLedgerRepository = sellerPayoutLedgerRepository;
        this.storeRepository = storeRepository;
        this.userContextUtil = userContextUtil;
        this.sellerContextUtil = sellerContextUtil;
    }

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            AddressRepository addressRepository,
            CustomerRepository customerRepository,
            CartService cartService,
            CheckoutService checkoutService,
            PdfInvoiceGeneratorService pdfInvoiceGeneratorService,
            StoreInvoiceSequenceService storeInvoiceSequenceService,
            SellerPayoutLedgerRepository sellerPayoutLedgerRepository,
            StoreRepository storeRepository,
            UserContextUtil userContextUtil,
            SellerContextUtil sellerContextUtil
    ) {
        this(orderRepository, orderItemRepository, productRepository, addressRepository, customerRepository,
                cartService, checkoutService, null, pdfInvoiceGeneratorService, storeInvoiceSequenceService,
                sellerPayoutLedgerRepository, storeRepository, userContextUtil, sellerContextUtil);
    }

    @Override
    public OrderResponse createOrder(Integer userId, OrderCreateRequest request) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        CartResponse cart = cartService.getCart(uid);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order with an empty cart");
        }

        // 1. Resolve primary store if specified on cart (for single-store compatibility and strict validation)
        Integer cartStoreId = cart.getStoreId();
        Store cartPrimaryStore = null;
        if (cartStoreId != null) {
            cartPrimaryStore = storeRepository.findById(cartStoreId).orElse(null);
        }
        if (cartPrimaryStore != null) {
            if (cartPrimaryStore.getStatus() != StoreStatus.ACTIVE) {
                throw new IllegalStateException("Cannot checkout: Store '" + cartPrimaryStore.getName() + "' is currently " + cartPrimaryStore.getStatus() + " and not accepting orders.");
            }
            boolean isHinchStoreOrder = (platformMarketplaceProperties != null)
                    ? platformMarketplaceProperties.isPlatformStore(cartPrimaryStore.getStoreId())
                    : (cartPrimaryStore.getStoreId() != null && cartPrimaryStore.getStoreId().equals(1));
            boolean checkoutEnabled = (platformMarketplaceProperties != null) && platformMarketplaceProperties.isHinchStoreCheckoutEnabled();
            if (isHinchStoreOrder && !checkoutEnabled) {
                throw new IllegalStateException("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");
            }
        }

        // 2. Resolve products, validate store status and stock availability, partition items by store
        Map<Integer, Product> productMap = new HashMap<>();
        Map<Integer, List<CartItemResponse>> storeItemsMap = new java.util.LinkedHashMap<>();
        Map<Integer, Store> storeMap = new HashMap<>();
        Map<Integer, Store> itemStoreMap = new HashMap<>();

        for (CartItemResponse ci : cart.getItems()) {
            Product p = productRepository.findById(ci.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + ci.getTitle() + " (ID: " + ci.getProductId() + ")"));

            if (p.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new IllegalStateException("Product '" + p.getTitle() + "' cannot be ordered because it is not approved by admin (status: " + p.getApprovalStatus() + ").");
            }
            if (!p.isActive()) {
                throw new IllegalStateException("Product '" + p.getTitle() + "' is no longer active in the catalog.");
            }

            Store store = null;
            if (cartPrimaryStore != null) {
                store = cartPrimaryStore;
            } else if (p.getStore() != null) {
                store = p.getStore();
            } else if (p.getSeller() != null && p.getSeller().getSellerId() != null) {
                store = storeRepository.findBySeller_SellerId(p.getSeller().getSellerId()).orElse(null);
            }
            if (store == null) {
                store = storeRepository.findById(1)
                        .or(() -> storeRepository.findAll().stream().filter(s -> s.getStatus() == StoreStatus.ACTIVE).findFirst())
                        .or(() -> storeRepository.findAll().stream().findFirst())
                        .orElseThrow(() -> new ResourceNotFoundException("No active store available for checkout."));
            }

            if (store.getStatus() != StoreStatus.ACTIVE) {
                throw new IllegalStateException("Cannot checkout: Store '" + store.getName() + "' is currently " + store.getStatus() + " and not accepting orders.");
            }

            boolean isHinchStore = (platformMarketplaceProperties != null)
                    ? platformMarketplaceProperties.isPlatformStore(store.getStoreId())
                    : (store.getStoreId() != null && store.getStoreId().equals(1));

            boolean checkoutEnabled = (platformMarketplaceProperties != null) && platformMarketplaceProperties.isHinchStoreCheckoutEnabled();
            if (isHinchStore && !checkoutEnabled) {
                throw new IllegalStateException("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");
            }

            if (!isHinchStore) {
                int availableStock = (productAvailabilityService != null)
                        ? productAvailabilityService.getAvailability(p).getAvailableQty()
                        : (p.getStockQty() != null ? p.getStockQty() : 0);
                if (availableStock < ci.getQuantity()) {
                    throw new ResourceConflictException("Insufficient stock for '" + p.getTitle() + "'. Requested: " + ci.getQuantity() + ", Available: " + availableStock);
                }
            }

            productMap.put(ci.getProductId(), p);
            storeMap.put(store.getStoreId(), store);
            itemStoreMap.put(ci.getProductId(), store);
            storeItemsMap.computeIfAbsent(store.getStoreId(), k -> new ArrayList<>()).add(ci);
        }

        // 3. Resolve customer and address
        Customer customer = customerRepository.findById(uid)
                .orElseGet(() -> Customer.builder().customerId(uid).build());

        Address address;
        if (request.getAddressId() != null) {
            address = addressRepository.findByCustomer_CustomerIdAndId(uid, request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));
        } else {
            address = addressRepository.findByCustomer_CustomerIdAndIsDefaultTrue(uid)
                    .orElseGet(() -> addressRepository.findFirstByCustomer_CustomerIdOrderByCreatedAtDesc(uid)
                            .orElseThrow(() -> new ResourceNotFoundException("No delivery address found for user ID: " + uid)));
        }

        // 4. Preview checkout to obtain per-store taxes, freight, and discount allocation
        CheckoutPreviewRequest previewReq = CheckoutPreviewRequest.builder()
                .addressId(address.getId())
                .deliverySlot(request.getDeliverySlot())
                .requiresCraneUnloading(request.getRequiresCraneUnloading())
                .build();
        CheckoutPreviewResponse preview = (checkoutService != null) ? checkoutService.previewCheckout(uid, previewReq) : null;
        if (preview != null && Boolean.FALSE.equals(preview.getCanPlaceOrder())) {
            throw new IllegalStateException(preview.getOrderBlockReason() != null
                    ? preview.getOrderBlockReason()
                    : "Cannot place order due to unavailable items.");
        }

        // 2. Deadlock prevention: sort all items ascending by productId and perform atomic reservations
        List<CartItemResponse> sortedCartItems = new ArrayList<>(cart.getItems());
        sortedCartItems.sort(Comparator.comparing(CartItemResponse::getProductId));

        List<CartItemResponse> reservedNonHinch = new ArrayList<>();
        Map<Integer, Store> hinchFulfillmentStoreMap = new HashMap<>();
        Map<Integer, List<CartItemResponse>> hinchReservedByStore = new HashMap<>();

        try {
            // Reserve non-HinchStore items (vendor-own)
            for (CartItemResponse ci : sortedCartItems) {
                Product p = productMap.get(ci.getProductId());
                Store s = itemStoreMap.get(ci.getProductId());
                if (s == null) {
                    s = storeMap.get(1);
                }
                boolean isHinch = (platformMarketplaceProperties != null && s != null)
                        ? platformMarketplaceProperties.isPlatformStore(s.getStoreId())
                        : (s != null && s.getStoreId() != null && s.getStoreId().equals(1));

                if (!isHinch) {
                    decrementStock(ci);
                    reservedNonHinch.add(ci);
                }
            }

            // Reserve HinchStore consignment items if any
            for (Map.Entry<Integer, List<CartItemResponse>> entry : storeItemsMap.entrySet()) {
                Integer sId = entry.getKey();
                Store s = storeMap.get(sId);
                boolean isHinch = (platformMarketplaceProperties != null)
                        ? platformMarketplaceProperties.isPlatformStore(s.getStoreId())
                        : (s.getStoreId() != null && s.getStoreId().equals(1));

                if (isHinch) {
                    if (vendorSelectionService == null) {
                        throw new IllegalStateException("VendorSelectionService is required for HinchStore consignment order fulfillment.");
                    }
                    List<CartItemResponse> sItemsSorted = new ArrayList<>(entry.getValue());
                    sItemsSorted.sort(Comparator.comparing(CartItemResponse::getProductId));

                    List<Store> candidates = vendorSelectionService.getRankedCandidateVendors(sItemsSorted, address);
                    if (candidates == null || candidates.isEmpty()) {
                        throw new ResourceConflictException("No consignment vendor has sufficient stock to fulfill this order.");
                    }

                    Store chosenVendor = null;
                    List<CartItemResponse> candidateReserved = new ArrayList<>();
                    for (Store candidateStore : candidates) {
                        boolean allReserved = true;
                        candidateReserved.clear();
                        for (CartItemResponse ci : sItemsSorted) {
                            int rows = (hinchStoreVendorInventoryRepository != null)
                                    ? hinchStoreVendorInventoryRepository.atomicReserve(ci.getProductId(), candidateStore.getStoreId(), ci.getQuantity())
                                    : 0;
                            if (rows > 0) {
                                candidateReserved.add(ci);
                            } else {
                                allReserved = false;
                                break;
                            }
                        }
                        if (allReserved) {
                            chosenVendor = candidateStore;
                            break;
                        } else {
                            if (hinchStoreVendorInventoryRepository != null) {
                                for (CartItemResponse ci : candidateReserved) {
                                    hinchStoreVendorInventoryRepository.atomicRelease(ci.getProductId(), candidateStore.getStoreId(), ci.getQuantity());
                                }
                            }
                            log.warn("Candidate vendor store #{} failed atomic reservation. Retrying next candidate...", candidateStore.getStoreId());
                        }
                    }

                    if (chosenVendor == null) {
                        throw new ResourceConflictException("Consignment stock reservation failed across all candidate vendor stores. Items may be out of stock.");
                    }
                    hinchFulfillmentStoreMap.put(sId, chosenVendor);
                    hinchReservedByStore.put(sId, new ArrayList<>(candidateReserved));
                }
            }
        } catch (RuntimeException ex) {
            // Atomic rollback across all stores
            for (CartItemResponse ci : reservedNonHinch) {
                try {
                    restoreStock(ci.getProductId(), ci.getQuantity());
                } catch (Exception ignore) {}
            }
            if (hinchStoreVendorInventoryRepository != null) {
                for (Map.Entry<Integer, Store> hEntry : hinchFulfillmentStoreMap.entrySet()) {
                    Store fStore = hEntry.getValue();
                    List<CartItemResponse> itemsToRelease = hinchReservedByStore.get(hEntry.getKey());
                    if (itemsToRelease != null) {
                        for (CartItemResponse ci : itemsToRelease) {
                            try {
                                hinchStoreVendorInventoryRepository.atomicRelease(ci.getProductId(), fStore.getStoreId(), ci.getQuantity());
                            } catch (Exception ignore) {}
                        }
                    }
                }
            }
            throw ex;
        }

        // Format delivery address string
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        StringBuilder sb = new StringBuilder();
        if (address.getSiteName() != null && !address.getSiteName().isBlank()) {
            sb.append(address.getSiteName()).append(", ");
        }
        if (address.getHouseFlatNo() != null && !address.getHouseFlatNo().isBlank()) {
            sb.append(address.getHouseFlatNo()).append(", ");
        }
        sb.append(address.getAddressLine1());
        if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) {
            sb.append(", ").append(address.getAddressLine2());
        }
        if (address.getAreaLocality() != null && !address.getAreaLocality().isBlank()) {
            sb.append(", ").append(address.getAreaLocality());
        }
        sb.append(", ").append(address.getCity()).append(", ").append(address.getState()).append(" - ").append(address.getPincode());
        boolean isForOther = Boolean.TRUE.equals(request.getOrderForSomeoneElse())
                || (request.getRecipientName() != null && !request.getRecipientName().isBlank())
                || (request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank());

        String effectiveRecipientName = (request.getRecipientName() != null && !request.getRecipientName().isBlank())
                ? request.getRecipientName().trim()
                : address.getRecipientName();

        String effectiveRecipientPhone = (request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank())
                ? request.getRecipientPhone().trim()
                : address.getPhone();

        if (effectiveRecipientPhone != null && effectiveRecipientPhone.length() > 50) {
            effectiveRecipientPhone = effectiveRecipientPhone.substring(0, 50).trim();
        }

        if (effectiveRecipientName != null && !effectiveRecipientName.isBlank()) {
            sb.append(" (Contact: ").append(effectiveRecipientName);
            if (effectiveRecipientPhone != null && !effectiveRecipientPhone.isBlank()) {
                sb.append(" / ").append(effectiveRecipientPhone);
            }
            if (isForOther) {
                sb.append(" [Order for Someone Else]");
            }
            sb.append(")");
        }
        String formattedAddress = sb.toString();

        // 3. Create parent OrderGroup
        String groupOrderNumber = "GRP-" + dateStr + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        BigDecimal groupSubtotal = preview != null && preview.getSubtotal() != null ? preview.getSubtotal() : BigDecimal.ZERO;
        BigDecimal groupDiscount = preview != null && preview.getDiscount() != null ? preview.getDiscount() : BigDecimal.ZERO;
        BigDecimal groupTaxable = preview != null && preview.getTaxableAmount() != null ? preview.getTaxableAmount() : BigDecimal.ZERO;
        BigDecimal groupCgst = preview != null && preview.getCgst() != null ? preview.getCgst() : BigDecimal.ZERO;
        BigDecimal groupSgst = preview != null && preview.getSgst() != null ? preview.getSgst() : BigDecimal.ZERO;
        BigDecimal groupIgst = preview != null && preview.getIgst() != null ? preview.getIgst() : BigDecimal.ZERO;
        BigDecimal groupTotalGst = preview != null && preview.getTotalGst() != null ? preview.getTotalGst() : BigDecimal.ZERO;
        BigDecimal groupFreight = preview != null && preview.getFreightCharge() != null ? preview.getFreightCharge() : BigDecimal.ZERO;
        BigDecimal groupCrane = preview != null && preview.getCraneUnloadingCharge() != null ? preview.getCraneUnloadingCharge() : BigDecimal.ZERO;
        BigDecimal groupGrandTotal = preview != null && preview.getGrandTotal() != null ? preview.getGrandTotal() : BigDecimal.ZERO;

        OrderGroup orderGroup = OrderGroup.builder()
                .groupOrderNumber(groupOrderNumber)
                .customer(customer)
                .addressId(address.getId())
                .deliveryLocation(formattedAddress)
                .orderForSomeoneElse(isForOther)
                .recipientName(effectiveRecipientName)
                .recipientPhone(effectiveRecipientPhone)
                .subtotal(groupSubtotal)
                .totalDiscount(groupDiscount)
                .couponCode(cart.getAppliedCoupon())
                .taxableAmount(groupTaxable)
                .cgst(groupCgst)
                .sgst(groupSgst)
                .igst(groupIgst)
                .totalGst(groupTotalGst)
                .totalFreight(groupFreight)
                .totalCraneCharge(groupCrane)
                .grandTotal(groupGrandTotal)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "RAZORPAY")
                .paymentStatus("PENDING")
                .groupStatus("PLACED")
                .build();
        if (orderGroupRepository != null) {
            orderGroup = orderGroupRepository.save(orderGroup);
        }

        // 4. Create separate child Order records per store
        Map<Integer, CartStoreGroupResponse> previewGroupMap = new HashMap<>();
        if (preview != null && preview.getStoreGroups() != null) {
            for (CartStoreGroupResponse grp : preview.getStoreGroups()) {
                if (grp.getStoreId() != null) {
                    previewGroupMap.put(grp.getStoreId(), grp);
                }
            }
        }

        List<Order> childOrders = new ArrayList<>();
        for (Map.Entry<Integer, List<CartItemResponse>> entry : storeItemsMap.entrySet()) {
            Integer sId = entry.getKey();
            Store store = storeMap.get(sId);
            List<CartItemResponse> storeItems = entry.getValue();
            storeItems.sort(Comparator.comparing(CartItemResponse::getProductId));

            CartStoreGroupResponse sg = previewGroupMap.get(sId);

            boolean isHinchStoreOrder = (platformMarketplaceProperties != null)
                    ? platformMarketplaceProperties.isPlatformStore(store.getStoreId())
                    : (store.getStoreId() != null && store.getStoreId().equals(1));

            Store fulfillmentStore = isHinchStoreOrder ? hinchFulfillmentStoreMap.get(sId) : store;
            OrderInventoryState inventoryState = isHinchStoreOrder ? OrderInventoryState.RESERVED : OrderInventoryState.NONE;

            String storeInvoiceNumber = storeInvoiceSequenceService.generateNextInvoiceNumber(store);
            String childOrderNumber = "ORD-" + dateStr + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

            BigDecimal sSubtotal = (sg != null && sg.getSubtotal() != null)
                    ? sg.getSubtotal()
                    : storeItems.stream().map(CartItemResponse::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sDiscount;
            if (sg != null && sg.getDiscount() != null) {
                sDiscount = sg.getDiscount();
            } else if (preview != null && preview.getDiscount() != null) {
                if (storeItemsMap.size() == 1) {
                    sDiscount = preview.getDiscount();
                } else if (preview.getSubtotal() != null && preview.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
                    sDiscount = preview.getDiscount().multiply(sSubtotal).divide(preview.getSubtotal(), 2, RoundingMode.HALF_UP);
                } else {
                    sDiscount = BigDecimal.ZERO;
                }
            } else {
                sDiscount = BigDecimal.ZERO;
            }

            BigDecimal sTaxable = (sg != null && sg.getTaxableAmount() != null)
                    ? sg.getTaxableAmount()
                    : ((preview != null && preview.getTaxableAmount() != null && storeItemsMap.size() == 1)
                            ? preview.getTaxableAmount()
                            : sSubtotal.subtract(sDiscount));

            BigDecimal sCgst = (sg != null && sg.getCgst() != null)
                    ? sg.getCgst()
                    : ((preview != null && preview.getCgst() != null && storeItemsMap.size() == 1)
                            ? preview.getCgst() : BigDecimal.ZERO);
            BigDecimal sSgst = (sg != null && sg.getSgst() != null)
                    ? sg.getSgst()
                    : ((preview != null && preview.getSgst() != null && storeItemsMap.size() == 1)
                            ? preview.getSgst() : BigDecimal.ZERO);
            BigDecimal sIgst = (sg != null && sg.getIgst() != null)
                    ? sg.getIgst()
                    : ((preview != null && preview.getIgst() != null && storeItemsMap.size() == 1)
                            ? preview.getIgst() : BigDecimal.ZERO);
            BigDecimal sTotalGst = (sg != null && sg.getTotalGst() != null)
                    ? sg.getTotalGst()
                    : ((preview != null && preview.getTotalGst() != null && storeItemsMap.size() == 1)
                            ? preview.getTotalGst() : sCgst.add(sSgst).add(sIgst));

            BigDecimal sFreight = (sg != null && sg.getDeliveryCharge() != null)
                    ? sg.getDeliveryCharge()
                    : ((preview != null && preview.getFreightCharge() != null && storeItemsMap.size() == 1)
                            ? preview.getFreightCharge() : BigDecimal.ZERO);
            BigDecimal sCrane = (sg != null && sg.getCraneUnloadingCharge() != null)
                    ? sg.getCraneUnloadingCharge()
                    : ((preview != null && preview.getCraneUnloadingCharge() != null && storeItemsMap.size() == 1)
                            ? preview.getCraneUnloadingCharge() : BigDecimal.ZERO);
            BigDecimal sTotalAmount = (sg != null && sg.getGrandTotal() != null)
                    ? sg.getGrandTotal()
                    : ((preview != null && preview.getGrandTotal() != null && storeItemsMap.size() == 1)
                            ? preview.getGrandTotal() : sTaxable.add(sTotalGst).add(sFreight).add(sCrane));

            Order childOrder = Order.builder()
                    .orderNumber(childOrderNumber)
                    .orderGroup(orderGroup)
                    .customer(customer)
                    .store(store)
                    .fulfillmentStore(fulfillmentStore)
                    .inventoryState(inventoryState)
                    .storeInvoiceNumber(storeInvoiceNumber)
                    .commissionRate(BigDecimal.ZERO)
                    .commissionAmount(BigDecimal.ZERO)
                    .addressId(address.getId())
                    .deliveryLocation(formattedAddress)
                    .orderForSomeoneElse(isForOther)
                    .recipientName(effectiveRecipientName)
                    .recipientPhone(effectiveRecipientPhone)
                    .subtotal(sSubtotal)
                    .discount(sDiscount)
                    .couponCode(cart.getAppliedCoupon())
                    .taxableAmount(sTaxable)
                    .cgst(sCgst)
                    .sgst(sSgst)
                    .igst(sIgst)
                    .totalGst(sTotalGst)
                    .freightCharge(sFreight)
                    .craneUnloadingCharge(sCrane)
                    .totalAmount(sTotalAmount)
                    .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "RAZORPAY")
                    .paymentStatus("PENDING")
                    .orderStatus("PLACED")
                    .poNumber(request.getPoNumber())
                    .deliverySlot(request.getDeliverySlot())
                    .deliveryInstructions(request.getDeliveryInstructions())
                    .requiresCraneUnloading(Boolean.TRUE.equals(request.getRequiresCraneUnloading()))
                    .build();

            Order savedChildOrder = orderRepository.save(childOrder);

            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal totalWeightKg = BigDecimal.ZERO;
            boolean hasAnyWeight = false;
            BigDecimal allocatedDiscountSum = BigDecimal.ZERO;
            int sItemCount = storeItems.size();

            for (int i = 0; i < sItemCount; i++) {
                CartItemResponse ci = storeItems.get(i);
                Product p = productMap.get(ci.getProductId());
                BigDecimal itemWeight = p != null ? p.getWeightKg() : null;

                BigDecimal itemDiscount = BigDecimal.ZERO;
                if (sDiscount.compareTo(BigDecimal.ZERO) > 0 && sSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                    if (i == sItemCount - 1) {
                        itemDiscount = sDiscount.subtract(allocatedDiscountSum);
                    } else {
                        itemDiscount = sDiscount.multiply(ci.getLineTotal())
                                .divide(sSubtotal, 2, RoundingMode.HALF_UP);
                        allocatedDiscountSum = allocatedDiscountSum.add(itemDiscount);
                    }
                }

                Seller itemSeller = null;
                if (isHinchStoreOrder) {
                    if (fulfillmentStore != null && fulfillmentStore.getSeller() != null) {
                        itemSeller = fulfillmentStore.getSeller();
                    } else if (p != null && p.getSeller() != null) {
                        itemSeller = p.getSeller();
                    }
                } else {
                    if (p != null && p.getSeller() != null) {
                        itemSeller = p.getSeller();
                    } else if (store != null && store.getSeller() != null) {
                        itemSeller = store.getSeller();
                    } else if (fulfillmentStore != null && fulfillmentStore.getSeller() != null) {
                        itemSeller = fulfillmentStore.getSeller();
                    }
                }

                if (!isHinchStoreOrder && itemSeller == null) {
                    throw new IllegalStateException("Order item for product '" + (p != null ? p.getTitle() : ci.getTitle())
                            + "' has no associated seller. Seller ID is required for vendor products.");
                }

                ItemSettlementSnapshot settlement = null;
                if (settlementCalculationService != null) {
                    Integer categoryId = null;
                    if (p != null) {
                        if (p.getSubcategory() != null && p.getSubcategory().getCategory() != null) {
                            categoryId = p.getSubcategory().getCategory().getCategoryId();
                        } else if (p.getBrand() != null && p.getBrand().getSubcategory() != null && p.getBrand().getSubcategory().getCategory() != null) {
                            categoryId = p.getBrand().getSubcategory().getCategory().getCategoryId();
                        }
                    }
                    settlement = settlementCalculationService.calculateItemSettlement(
                            isHinchStoreOrder,
                            ci.getLineTotal(),
                            itemDiscount,
                            categoryId
                    );
                }

                if (!isHinchStoreOrder && itemDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("Coupon applied to vendor-own item: orderId={}, orderNumber={}, productId={}, couponAmount={}. Platform funds full vendor settlement.",
                            savedChildOrder.getOrderId(), savedChildOrder.getOrderNumber(), ci.getProductId(), itemDiscount);
                }

                BigDecimal commissionRate = settlement != null ? settlement.commissionRate() : BigDecimal.ZERO;
                BigDecimal sellerPayable = settlement != null ? settlement.sellerPayable() : ci.getLineTotal();
                BigDecimal hinchKeeps = settlement != null ? settlement.hinchKeeps() : BigDecimal.ZERO;

                OrderItem oi = OrderItem.builder()
                        .order(savedChildOrder)
                        .productId(ci.getProductId())
                        .title(ci.getTitle())
                        .imageUrl(ci.getImageUrl())
                        .quantity(ci.getQuantity())
                        .unit(ci.getUnit())
                        .weightKg(itemWeight)
                        .unitPrice(ci.getUnitPrice())
                        .originalPrice(ci.getOriginalPrice())
                        .appliedTier(ci.getAppliedTier())
                        .gstRate(ci.getGstRate())
                        .lineTotal(ci.getLineTotal())
                        .lineGst(ci.getLineGst())
                        .seller(itemSeller)
                        .fulfillmentStore(fulfillmentStore)
                        .isHinchStore(isHinchStoreOrder && itemSeller == null)
                        .commissionRate(commissionRate)
                        .sellerPayable(sellerPayable)
                        .hinchKeeps(hinchKeeps)
                        .build();
                OrderItem savedOi = orderItemRepository.save(oi);
                orderItems.add(savedOi);

                if (savedOi.getWeightKg() != null && savedOi.getQuantity() != null) {
                    totalWeightKg = totalWeightKg.add(savedOi.getWeightKg().multiply(BigDecimal.valueOf(savedOi.getQuantity())));
                    hasAnyWeight = true;
                }
            }
            savedChildOrder.setItems(orderItems);
            savedChildOrder.setTotalWeightKg(hasAnyWeight ? totalWeightKg : null);

            BigDecimal totalHinchKeeps = orderItems.stream()
                    .map(OrderItem::getHinchKeeps)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            savedChildOrder.setCommissionAmount(totalHinchKeeps);
            savedChildOrder = orderRepository.save(savedChildOrder);

            // SellerPayoutLedger entries per-item
            for (OrderItem oi : orderItems) {
                BigDecimal sellerPayable = oi.getSellerPayable();
                BigDecimal tcsAmount = (isHinchStoreOrder || Boolean.TRUE.equals(oi.getIsHinchStore()))
                        ? BigDecimal.ZERO
                        : sellerPayable.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal netPayout = sellerPayable.subtract(tcsAmount);

                PayoutLedgerStatus initialStatus = (isHinchStoreOrder && oi.getSeller() == null)
                        ? PayoutLedgerStatus.ON_HOLD
                        : PayoutLedgerStatus.PENDING;

                SellerPayoutLedger ledger = SellerPayoutLedger.builder()
                        .store(fulfillmentStore != null ? fulfillmentStore : store)
                        .seller(oi.getSeller())
                        .order(savedChildOrder)
                        .orderItem(oi)
                        .grossAmount(sellerPayable)
                        .commissionAmount(BigDecimal.ZERO)
                        .hinchKeeps(oi.getHinchKeeps())
                        .tcsAmount(tcsAmount)
                        .netPayoutAmount(netPayout)
                        .status(initialStatus)
                        .build();
                sellerPayoutLedgerRepository.save(ledger);
            }

            // Initial tracking checkpoint per store order
            String logisticsHub = (fulfillmentStore != null ? fulfillmentStore.getName() : store.getName()) + " Logistics Hub";
            String fulfillmentTeam = (fulfillmentStore != null ? fulfillmentStore.getName() : store.getName()) + " fulfillment team.";
            TrackingCheckpoint initialCheckpoint = TrackingCheckpoint.builder()
                    .order(savedChildOrder)
                    .status("ORDER_PLACED")
                    .title("Order Placed & Verified")
                    .location(logisticsHub)
                    .description("Order confirmed and assigned to " + fulfillmentTeam)
                    .timestamp(LocalDateTime.now())
                    .build();
            savedChildOrder.getCheckpoints().add(initialCheckpoint);
            orderRepository.save(savedChildOrder);

            childOrders.add(savedChildOrder);
            log.info("Successfully created child Order #{} (Invoice: {}) for Customer #{} from Store #{} ('{}')",
                    savedChildOrder.getOrderId(), storeInvoiceNumber, uid, store.getStoreId(), store.getName());
        }

        if (orderGroup != null) {
            orderGroup.setOrders(childOrders);
            if (orderGroupRepository != null) {
                orderGroup = orderGroupRepository.save(orderGroup);
            }
        }

        // Record coupon usage linking orderGroup and first child order
        if (cart.getAppliedCoupon() != null && !cart.getAppliedCoupon().isBlank() && !childOrders.isEmpty()) {
            couponService.recordCouponUsage(cart.getAppliedCoupon(), customer, orderGroup, childOrders.get(0), preview.getDiscount());
        }

        // Clear active cart
        cartService.clearCart(uid);

        // Build composite response
        List<OrderResponse> childResponses = childOrders.stream().map(this::mapToOrderResponse).toList();
        Order primaryOrder = childOrders.get(0);
        OrderResponse rootResponse = mapToOrderResponse(primaryOrder);
        Integer ogId = (orderGroup != null && orderGroup.getId() != null) ? orderGroup.getId() : primaryOrder.getOrderId();
        String ogNum = (orderGroup != null && orderGroup.getGroupOrderNumber() != null) ? orderGroup.getGroupOrderNumber() : primaryOrder.getOrderNumber();
        rootResponse.setOrderGroupId(ogId);
        rootResponse.setGroupOrderNumber(ogNum);
        rootResponse.setChildOrders(childResponses);

        if (childOrders.size() > 1 && orderGroup != null) {
            rootResponse.setOrderNumber(orderGroup.getGroupOrderNumber());
            rootResponse.setStoreId(null);
            rootResponse.setStoreName("Multiple Stores (" + childOrders.size() + ")");
            rootResponse.setStoreSlug(null);
            rootResponse.setSubtotal(orderGroup.getSubtotal());
            rootResponse.setDiscount(orderGroup.getTotalDiscount());
            rootResponse.setTaxableAmount(orderGroup.getTaxableAmount());
            rootResponse.setCgst(orderGroup.getCgst());
            rootResponse.setSgst(orderGroup.getSgst());
            rootResponse.setIgst(orderGroup.getIgst());
            rootResponse.setTotalGst(orderGroup.getTotalGst());
            rootResponse.setFreightCharge(orderGroup.getTotalFreight());
            rootResponse.setCraneUnloadingCharge(orderGroup.getTotalCraneCharge());
            rootResponse.setTotalAmount(orderGroup.getGrandTotal());

            List<OrderResponse.OrderItemDto> allItems = new ArrayList<>();
            for (OrderResponse cr : childResponses) {
                if (cr.getItems() != null) {
                    allItems.addAll(cr.getItems());
                }
            }
            rootResponse.setItems(allItems);
            rootResponse.setItemCount(allItems.size());
        }

        return rootResponse;
    }

    private synchronized void decrementStock(CartItemResponse cartItem) {
        Product product = productRepository.findByIdForStockUpdate(cartItem.getProductId())
                .or(() -> productRepository.findById(cartItem.getProductId()))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItem.getProductId()));

        int currentStock = product.getStockQty() != null ? product.getStockQty() : 0;
        int requestedQty = cartItem.getQuantity();

        if (currentStock < requestedQty) {
            throw new ResourceConflictException("Insufficient stock for product '" + product.getTitle()
                    + "'. Available: " + currentStock + ", Requested: " + requestedQty);
        }

        product.setStockQty(currentStock - requestedQty);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderSummaryResponse>> getOrders(Integer userId, String status, int page, int limit) {
        if (SecurityUtils.isAdmin()) {
            return getAllOrdersForAdmin(status, page, limit);
        }

        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        if (SecurityUtils.isSeller()) {
            Integer sellerId = SecurityUtils.getCurrentSellerId()
                    .or(() -> {
                        try {
                            return Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    })
                    .orElseThrow(() -> new UnauthorizedException("Seller identity could not be resolved."));

            Page<Order> orderPage = (status != null && !status.trim().isEmpty())
                    ? orderRepository.findSellerOrdersByStatus(sellerId, status.trim().toUpperCase(), pageable)
                    : orderRepository.findSellerOrders(sellerId, pageable);

            List<OrderSummaryResponse> summaryList = orderPage.getContent().stream()
                    .map(this::mapToSummaryResponse)
                    .toList();
            PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, orderPage.getTotalElements());
            return ApiResponse.paginated("Orders retrieved successfully", summaryList, meta);
        }

        if (userId == null) {
            throw new UnauthorizedException("Authentication required: User ID must not be null.");
        }
        int uid = userId;
        Page<Order> orderPage = (status != null && !status.trim().isEmpty())
                ? orderRepository.findByCustomer_CustomerIdAndOrderStatusIgnoreCaseOrderByCreatedAtDesc(uid, status.trim().toUpperCase(), pageable)
                : orderRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(uid, pageable);

        List<OrderSummaryResponse> summaryList = orderPage.getContent().stream()
                .map(this::mapToSummaryResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, orderPage.getTotalElements());
        return ApiResponse.paginated("Orders retrieved successfully", summaryList, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderSummaryResponse>> getAllOrdersForAdmin(String status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<Order> orderPage = (status != null && !status.trim().isEmpty())
                ? orderRepository.findByOrderStatusIgnoreCaseOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable)
                : orderRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<OrderSummaryResponse> summaryList = orderPage.getContent().stream()
                .map(this::mapToSummaryResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, orderPage.getTotalElements());
        return ApiResponse.paginated("Orders retrieved successfully", summaryList, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        List<OrderTrackingResponse.TrackingEvent> events = order.getCheckpoints().stream()
                .map(c -> OrderTrackingResponse.TrackingEvent.builder()
                        .checkpointId(c.getCheckpointId())
                        .status(c.getStatus())
                        .title(c.getTitle())
                        .location(c.getLocation())
                        .description(c.getDescription())
                        .timestamp(c.getTimestamp())
                        .build())
                .toList();

        return OrderTrackingResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .currentStatus(order.getOrderStatus())
                .trackingNumber(order.getTrackingNumber())
                .carrierName(order.getCarrierName())
                .vehicleNumber(order.getVehicleNumber())
                .driverName(order.getDriverName())
                .estimatedDelivery(order.getEstimatedDelivery())
                .checkpoints(events)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getOrderInvoice(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        Customer customer = order.getCustomer();

        Store store = order.getStore();
        String sellerCompanyName = (store != null && store.getSeller() != null && store.getSeller().getCompanyName() != null)
                ? store.getSeller().getCompanyName()
                : (store != null ? store.getName() : "HinchMart B2B Commerce Pvt Ltd");
        String sellerGstin = (store != null && store.getSeller() != null && store.getSeller().getGstin() != null)
                ? store.getSeller().getGstin() : "36AAACH2026Q1Z1";

        List<InvoiceResponse.InvoiceItem> invoiceItems = order.getItems().stream()
                .map(item -> {
                    Product product = (item.getProductId() != null && item.getProductId() > 0)
                            ? productRepository.findById(item.getProductId()).orElse(null)
                            : null;
                    String hsn = (product != null && product.getHsnCode() != null && !product.getHsnCode().isBlank())
                            ? product.getHsnCode().trim() : "7214";
                    return InvoiceResponse.InvoiceItem.builder()
                            .itemId(item.getOrderItemId())
                            .description(item.getTitle())
                            .hsnCode(hsn)
                            .quantity(item.getQuantity())
                            .unit(item.getUnit())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .gstRate(item.getGstRate())
                            .gstAmount(item.getLineGst())
                            .build();
                })
                .toList();

        String invNum = (order.getStoreInvoiceNumber() != null && !order.getStoreInvoiceNumber().isBlank())
                ? order.getStoreInvoiceNumber() : "INV-" + String.format("%06d", order.getOrderId());

        boolean isInterState = order.getIgst() != null && order.getIgst().compareTo(BigDecimal.ZERO) > 0;
        String placeOfSupply = isInterState ? "Inter-State (IGST)" : "Telangana (36)";

        return InvoiceResponse.builder()
                .invoiceNumber(invNum)
                .invoiceDate(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")))
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .supplierName(sellerCompanyName)
                .supplierGstin(sellerGstin)
                .supplierAddress("HITEC City, Hyderabad, Telangana - 500081")
                .recipientName(customer != null && customer.getName() != null ? customer.getName() : "Enterprise Customer")
                .recipientAddress(order.getDeliveryLocation())
                .recipientGstin("36AAACT2727Q1ZW")
                .placeOfSupply(placeOfSupply)
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .taxableAmount(order.getTaxableAmount())
                .cgst(order.getCgst())
                .sgst(order.getSgst())
                .igst(order.getIgst())
                .totalGst(order.getTotalGst())
                .freightCharge(order.getFreightCharge())
                .craneUnloadingCharge(order.getCraneUnloadingCharge())
                .grandTotal(order.getTotalAmount())
                .items(invoiceItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);
        Customer customer = order.getCustomer();
        String invoiceNumber = (order.getStoreInvoiceNumber() != null && !order.getStoreInvoiceNumber().isBlank())
                ? order.getStoreInvoiceNumber() : "INV-" + String.format("%06d", order.getOrderId());
        return pdfInvoiceGeneratorService.generateInvoicePdf(order, customer, invoiceNumber);
    }

    @Override
    public OrderResponse updateOrderStatus(Integer id, String status, String location, String description) {
        return updateOrderStatus(id, status, location, description, null, null, null, null, null);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer id, String status, String location, String description,
                                           String carrierName, String vehicleNumber, String driverName,
                                           String trackingNumber, String estimatedDelivery) {
        Order order = orderRepository.findByIdForUpdate(id)
                .or(() -> orderRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        validateOrderStatusUpdateAccess(order);
        String upperStatus = status.trim().toUpperCase();

        if ("DELIVERED".equals(upperStatus)) {
            if (!SecurityUtils.isAdmin()) {
                throw new ForbiddenException("Access denied: Only administrators can mark an order as DELIVERED.");
            }

            // Idempotency: Already DELIVERED returns success with no changes
            if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
                log.info("Order #{} is already DELIVERED. Returning existing order response idempotently.", id);
                return mapToOrderResponse(order);
            }

            // Allowed only from DISPATCHED or IN_TRANSIT
            String currentStatus = order.getOrderStatus() != null ? order.getOrderStatus().trim().toUpperCase() : "";
            if (!"DISPATCHED".equals(currentStatus) && !"IN_TRANSIT".equals(currentStatus)) {
                throw new IllegalArgumentException("Order #" + id + " cannot be marked DELIVERED from status '" + currentStatus
                        + "'. Allowed prior statuses are DISPATCHED or IN_TRANSIT.");
            }
        }

        order.setOrderStatus(upperStatus);

        if (carrierName != null && !carrierName.isBlank()) {
            order.setCarrierName(carrierName.trim());
        }
        if (vehicleNumber != null && !vehicleNumber.isBlank()) {
            order.setVehicleNumber(vehicleNumber.trim());
        }
        if (driverName != null && !driverName.isBlank()) {
            order.setDriverName(driverName.trim());
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            order.setTrackingNumber(trackingNumber.trim());
        }
        if (estimatedDelivery != null && !estimatedDelivery.isBlank()) {
            try {
                order.setEstimatedDelivery(LocalDateTime.parse(estimatedDelivery.trim()));
            } catch (Exception e) {
                log.warn("Invalid estimatedDelivery format for order #{}: {}", id, estimatedDelivery);
            }
        }

        String checkpointDesc = description != null && !description.isBlank() ? description : "Status updated to " + upperStatus;
        if ("DELIVERED".equals(upperStatus)) {
            String deliveredBy = driverName != null && !driverName.isBlank() ? driverName.trim()
                    : (carrierName != null && !carrierName.isBlank() ? carrierName.trim() : null);
            if (deliveredBy != null) {
                checkpointDesc = checkpointDesc + " (Delivered by: " + deliveredBy + ")";
            }
        }

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status(upperStatus)
                .title(formatCheckpointTitle(upperStatus))
                .location(location != null ? location : ("DELIVERED".equals(upperStatus) ? "Customer Jobsite" : "In Transit Hub"))
                .description(checkpointDesc)
                .timestamp(LocalDateTime.now())
                .build();

        order.getCheckpoints().add(cp);
        Order updated = orderRepository.save(order);

        if ("DELIVERED".equalsIgnoreCase(upperStatus)) {
            List<SellerPayoutLedger> ledgers = sellerPayoutLedgerRepository.findAllByOrder_OrderId(order.getOrderId());
            if (ledgers.isEmpty()) {
                sellerPayoutLedgerRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(ledgers::add);
            }
            for (SellerPayoutLedger ledger : ledgers) {
                if (ledger.getStatus() == PayoutLedgerStatus.PENDING) {
                    if (ledger.getSettlementDate() == null) {
                        ledger.setSettlementDate(LocalDateTime.now().plusDays(7));
                        sellerPayoutLedgerRepository.save(ledger);
                        log.info("Set T+7 settlementDate for PayoutLedger #{} (Order #{}): {}",
                                ledger.getId(), order.getOrderId(), ledger.getSettlementDate());
                    }
                }
            }
        }

        return mapToOrderResponse(updated);
    }

    @Override
    public OrderResponse cancelOrder(Integer id, String location, String description) {
        Order order = findOrder(id);
        validateOrderCancelAccess(order);
        if ("DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Delivered orders cannot be cancelled directly. Please raise a return/dispute.");
        }

        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("REFUND_PENDING");

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("CANCELLED")
                .title("Order Cancelled")
                .location(location != null ? location : "Customer Request")
                .description(description != null ? description : "Order cancelled before dispatch.")
                .timestamp(LocalDateTime.now())
                .build();

        order.getCheckpoints().add(cp);
        Order updated = orderRepository.save(order);

        boolean isHinchOrder = (order.getStore() != null && platformMarketplaceProperties != null
                && platformMarketplaceProperties.isPlatformStore(order.getStore().getStoreId()))
                || (order.getStore() != null && Integer.valueOf(1).equals(order.getStore().getStoreId()));

        if (isHinchOrder) {
            releaseOrRestockHinchStoreInventory(order);
        } else {
            // Restore stock for vendor-own product
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    restoreStock(item);
                }
            }
        }

        // Mark all payout ledgers as REVERSED
        reverseOrderLedgers(order.getOrderId(), description != null ? description : "Pre-dispatch order cancellation by customer");

        // Publish cancellation event to trigger automatic customer refund
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.example.project.customer.event.OrderCancelledEvent(
                    updated.getOrderId(),
                    description != null ? description : "Pre-dispatch cancellation by customer"
            ));
        }

        // Update parent OrderGroup status if part of a group
        if (order.getOrderGroup() != null && order.getOrderGroup().getId() != null) {
            OrderGroup group = order.getOrderGroup();
            List<Order> siblings = orderRepository.findByOrderGroup_Id(group.getId());
            boolean allCancelled = siblings.isEmpty() || siblings.stream().allMatch(o -> "CANCELLED".equalsIgnoreCase(o.getOrderStatus()));
            if (allCancelled) {
                group.setGroupStatus("CANCELLED");
                group.setPaymentStatus("REFUND_PENDING");
            } else {
                group.setGroupStatus("PARTIALLY_CANCELLED");
            }
            if (orderGroupRepository != null) {
                orderGroupRepository.save(group);
            }
        }

        return mapToOrderResponse(updated);
    }

    @Override
    @Transactional
    public void commitHinchStoreReservation(Order order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        int rows = orderRepository.transitionInventoryState(
                order.getOrderId(),
                OrderInventoryState.RESERVED,
                OrderInventoryState.COMMITTED
        );
        if (rows > 0) {
            Store fulfillmentStore = order.getFulfillmentStore();
            if (fulfillmentStore != null && hinchStoreVendorInventoryRepository != null && order.getItems() != null) {
                List<OrderItem> items = new ArrayList<>(order.getItems());
                items.sort(Comparator.comparing(OrderItem::getProductId));
                for (OrderItem item : items) {
                    hinchStoreVendorInventoryRepository.atomicCommit(
                            item.getProductId(),
                            fulfillmentStore.getStoreId(),
                            item.getQuantity()
                    );
                }
            }
            order.setInventoryState(OrderInventoryState.COMMITTED);
            log.info("Committed inventory reservation for HinchStore Order #{}", order.getOrderId());
        } else {
            log.info("Order #{} inventory state transition RESERVED -> COMMITTED skipped (state is not RESERVED or already transitioned).", order.getOrderId());
        }
    }

    @Override
    @Transactional
    public void releaseOrRestockHinchStoreInventory(Order order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }

        if (order.getInventoryState() == OrderInventoryState.COMMITTED) {
            // Attempt COMMITTED -> RESTOCKED transition
            int rowsRestocked = orderRepository.transitionInventoryState(
                    order.getOrderId(),
                    OrderInventoryState.COMMITTED,
                    OrderInventoryState.RESTOCKED
            );
            if (rowsRestocked > 0) {
                Store fulfillmentStore = order.getFulfillmentStore();
                if (fulfillmentStore != null && hinchStoreVendorInventoryRepository != null && order.getItems() != null) {
                    List<OrderItem> items = new ArrayList<>(order.getItems());
                    items.sort(Comparator.comparing(OrderItem::getProductId));
                    for (OrderItem item : items) {
                        hinchStoreVendorInventoryRepository.atomicRestock(
                                item.getProductId(),
                                fulfillmentStore.getStoreId(),
                                item.getQuantity()
                        );
                    }
                }
                order.setInventoryState(OrderInventoryState.RESTOCKED);
                reverseOrderLedgers(order.getOrderId(), "Order cancelled post-commit, restocked at vendor store");
                log.info("Restocked committed inventory for Order #{}", order.getOrderId());
            }
            return;
        }

        // Attempt RESERVED -> RELEASED transition
        int rowsReleased = orderRepository.transitionInventoryState(
                order.getOrderId(),
                OrderInventoryState.RESERVED,
                OrderInventoryState.RELEASED
        );
        if (rowsReleased > 0) {
            Store fulfillmentStore = order.getFulfillmentStore();
            if (fulfillmentStore != null && hinchStoreVendorInventoryRepository != null && order.getItems() != null) {
                List<OrderItem> items = new ArrayList<>(order.getItems());
                items.sort(Comparator.comparing(OrderItem::getProductId));
                for (OrderItem item : items) {
                    hinchStoreVendorInventoryRepository.atomicRelease(
                            item.getProductId(),
                            fulfillmentStore.getStoreId(),
                            item.getQuantity()
                    );
                }
            }
            order.setInventoryState(OrderInventoryState.RELEASED);
            reverseOrderLedgers(order.getOrderId(), "Consignment reservation released");
            log.info("Released reserved inventory for Order #{}", order.getOrderId());
        }
    }

    private void reverseOrderLedgers(Integer orderId, String reason) {
        List<SellerPayoutLedger> ledgers = sellerPayoutLedgerRepository.findAllByOrder_OrderId(orderId);
        if (ledgers.isEmpty()) {
            sellerPayoutLedgerRepository.findByOrder_OrderId(orderId).ifPresent(ledgers::add);
        }
        for (SellerPayoutLedger ledger : ledgers) {
            if (ledger.getStatus() == PayoutLedgerStatus.PENDING) {
                ledger.setStatus(PayoutLedgerStatus.REVERSED);
                ledger.setClawbackReason(reason);
                sellerPayoutLedgerRepository.save(ledger);
                log.info("Payout ledger #{} for Order #{} marked as REVERSED (Reason: {})", ledger.getId(), orderId, reason);
            } else if (ledger.getStatus() == PayoutLedgerStatus.ELIGIBLE || ledger.getStatus() == PayoutLedgerStatus.SETTLED) {
                ledger.setClawbackReason(reason);
                sellerPayoutLedgerRepository.save(ledger);
                log.warn("ALERT: Order #{} cancelled/refunded but PayoutLedger #{} is already in state {} (settlementDate: {}). Status NOT overwritten; flagged with clawbackReason '{}' for manual clawback review.",
                        orderId, ledger.getId(), ledger.getStatus(), ledger.getSettlementDate(), reason);
            }
        }
    }

    @Override
    public com.example.project.customer.dto.OrderDisputeResponse raiseDispute(Integer id, com.example.project.customer.dto.OrderDisputeRequest request) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason() : "Damaged / Incorrect Goods";
        String desc = (request != null && request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription() : "Customer reported an issue with delivered goods.";

        order.setOrderStatus("RETURN_REQUESTED");

        TrackingCheckpoint cp = TrackingCheckpoint.builder()
                .order(order)
                .status("DISPUTE_RAISED")
                .title("Return / Dispute Requested")
                .location("Customer Support Desk")
                .description(reason + ": " + desc)
                .timestamp(LocalDateTime.now())
                .build();

        order.getCheckpoints().add(cp);
        orderRepository.save(order);

        String disputeId = "DISP-" + id + "-" + (System.currentTimeMillis() % 100000);
        log.info("Dispute {} created for Order #{} by customer with reason: {}", disputeId, id, reason);

        return com.example.project.customer.dto.OrderDisputeResponse.builder()
                .success(true)
                .disputeId(disputeId)
                .orderId(id)
                .status("RETURN_REQUESTED")
                .reason(reason)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public com.example.project.customer.dto.OrderMtcResponse getOrderMtc(Integer id) {
        Order order = findOrder(id);
        validateOrderReadAccess(order);

        String productName = "Structural Steel / Fe550D TMT Rebar";
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            productName = order.getItems().get(0).getTitle();
        }

        Map<String, String> chemical = Map.of(
                "Carbon (C)", "0.22% (Max 0.25%)",
                "Sulphur (S)", "0.035% (Max 0.040%)",
                "Phosphorus (P)", "0.038% (Max 0.040%)",
                "Carbon Equivalent (CE)", "0.38% (Max 0.42%)"
        );

        Map<String, String> mechanical = Map.of(
                "Yield Stress (0.2% Proof)", "568 N/mm² (Min 550 N/mm²)",
                "Ultimate Tensile Strength", "625 N/mm² (Min 585 N/mm²)",
                "Elongation (Gauge 5.65√A)", "16.5% (Min 14.5%)",
                "TS/YS Ratio", "1.10 (Min 1.08)"
        );

        String invNum = (order.getStoreInvoiceNumber() != null && !order.getStoreInvoiceNumber().isBlank())
                ? order.getStoreInvoiceNumber() : ("ORD-" + id);

        return com.example.project.customer.dto.OrderMtcResponse.builder()
                .orderId(id)
                .certificateNumber("MTC-" + invNum + "-QC")
                .productName(productName)
                .heatNumber("HT-" + (100000 + (id * 31 % 900000)))
                .batchNumber("LOT-2026-" + (1000 + id))
                .grade("IS 1786:2008 Fe550D High Ductility")
                .inspectionAgency("Bureau Veritas / TUV India NABL Certified Lab")
                .chemicalAnalysis(chemical)
                .mechanicalProperties(mechanical)
                .status("VERIFIED")
                .verified(true)
                .downloadUrl("/api/orders/" + id + "/invoice/pdf")
                .issuedAt(order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    private boolean isSellerAuthorizedForOrder(Order order, Integer sellerId) {
        if (order == null || sellerId == null) {
            return false;
        }
        // 1. Fulfilling vendor holding the stock
        if (order.getFulfillmentStore() != null && order.getFulfillmentStore().getSeller() != null
                && sellerId.equals(order.getFulfillmentStore().getSeller().getSellerId())) {
            return true;
        }
        // 2. Direct store seller
        if (order.getStore() != null && order.getStore().getSeller() != null
                && sellerId.equals(order.getStore().getSeller().getSellerId())) {
            return true;
        }
        // 3. Any item seller
        if (order.getItems() != null) {
            return order.getItems().stream()
                    .anyMatch(oi -> (oi.getSeller() != null && sellerId.equals(oi.getSeller().getSellerId()))
                            || isProductOwnedBySeller(oi.getProductId(), sellerId));
        }
        return false;
    }

    private boolean isProductOwnedBySeller(Integer productId, Integer sellerId) {
        if (productId == null || sellerId == null) return false;
        return productRepository.findById(productId)
                .map(Product::getSeller)
                .map(Seller::getSellerId)
                .filter(sellerId::equals)
                .isPresent();
    }

    private void validateOrderReadAccess(Order order) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = userContextUtil.getOptionalCurrentUserId();
        if (currentUserId != null && order.getCustomer() != null && currentUserId.equals(order.getCustomer().getCustomerId())) {
            return;
        }
        // Check if current user is the seller who owns the store or is fulfilling this order
        Optional<Integer> currentSellerId = SecurityUtils.getCurrentSellerId();
        if (currentSellerId.isEmpty()) {
            try {
                currentSellerId = Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
            } catch (Exception ignored) {}
        }
        if (currentSellerId.isPresent() && isSellerAuthorizedForOrder(order, currentSellerId.get())) {
            return;
        }
        throw new ForbiddenException("Access denied: You do not have permission to access Order #" + order.getOrderId());
    }

    private void validateOrderCancelAccess(Order order) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = null;
        try {
            currentUserId = userContextUtil.getOptionalCurrentUserId();
        } catch (Exception ignored) {}

        if (currentUserId != null && order.getCustomer() != null && currentUserId.equals(order.getCustomer().getCustomerId())) {
            return;
        }

        if (SecurityUtils.isSeller()) {
            Optional<Integer> currentSellerId = SecurityUtils.getCurrentSellerId();
            if (currentSellerId.isEmpty()) {
                try {
                    currentSellerId = Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
                } catch (Exception ignored) {}
            }
            if (currentSellerId.isPresent() && isSellerAuthorizedForOrder(order, currentSellerId.get())) {
                return;
            }
        }

        throw new ForbiddenException("Access denied: You can only cancel your own orders.");
    }

    private void validateOrderStatusUpdateAccess(Order order) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Optional<Integer> currentSellerId = SecurityUtils.getCurrentSellerId();
        if (currentSellerId.isEmpty()) {
            try {
                currentSellerId = Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
            } catch (Exception ignored) {}
        }
        if (currentSellerId.isPresent() && isSellerAuthorizedForOrder(order, currentSellerId.get())) {
            return;
        }
        throw new ForbiddenException("Access denied: Only administrators or the fulfilling store owner can update order status.");
    }

    private synchronized void restoreStock(OrderItem orderItem) {
        if (orderItem != null) {
            restoreStock(orderItem.getProductId(), orderItem.getQuantity());
        }
    }

    private synchronized void restoreStock(Integer productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        Product product = productRepository.findByIdForStockUpdate(productId)
                .or(() -> productRepository.findById(productId))
                .orElse(null);
        if (product != null) {
            int availableStock = product.getStockQty() != null ? product.getStockQty() : 0;
            product.setStockQty(availableStock + quantity);
            productRepository.save(product);
        }
    }

    private Order findOrder(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private String formatCheckpointTitle(String status) {
        return switch (status.toUpperCase()) {
            case "LOADED" -> "Loaded & Weighed at Weighbridge";
            case "DISPATCHED" -> "Dispatched from Distribution Hub";
            case "IN_TRANSIT" -> "Vehicle En Route to Jobsite";
            case "DELIVERED" -> "Delivered and Unloaded at Jobsite";
            case "CANCELLED" -> "Order Cancelled";
            default -> "Status Updated: " + status;
        };
    }

    private OrderSummaryResponse mapToSummaryResponse(Order o) {
        String firstTitle = null;
        String firstImage = null;
        int count = o.getItems() != null ? o.getItems().size() : 0;

        if (o.getItems() != null && !o.getItems().isEmpty()) {
            OrderItem first = o.getItems().get(0);
            firstTitle = first.getTitle();
            firstImage = first.getImageUrl();
        }

        Store store = o.getStore();
        Customer customer = o.getCustomer();
        String recName = (o.getRecipientName() != null && !o.getRecipientName().isBlank())
                ? o.getRecipientName()
                : (customer != null ? customer.getName() : null);
        String recPhone = (o.getRecipientPhone() != null && !o.getRecipientPhone().isBlank())
                ? o.getRecipientPhone()
                : (customer != null ? customer.getPhone() : null);

        return OrderSummaryResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .customerId(customer != null ? customer.getCustomerId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .storeId(store != null ? store.getStoreId() : null)
                .storeName(store != null ? store.getName() : null)
                .storeSlug(store != null ? store.getSlug() : null)
                .fulfillmentStoreId(o.getFulfillmentStore() != null ? o.getFulfillmentStore().getStoreId() : null)
                .fulfillmentStoreName(o.getFulfillmentStore() != null ? o.getFulfillmentStore().getName() : null)
                .inventoryState(o.getInventoryState() != null ? o.getInventoryState().name() : null)
                .storeInvoiceNumber(o.getStoreInvoiceNumber())
                .totalAmount(o.getTotalAmount())
                .orderStatus(o.getOrderStatus())
                .paymentStatus(o.getPaymentStatus())
                .orderForSomeoneElse(o.getOrderForSomeoneElse())
                .recipientName(recName)
                .recipientPhone(recPhone)
                .itemCount(count)
                .firstItemTitle(firstTitle)
                .firstItemImage(firstImage)
                .createdAt(o.getCreatedAt())
                .estimatedDelivery(o.getEstimatedDelivery())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order o) {
        List<OrderResponse.OrderItemDto> itemDtos = o.getItems() != null ? o.getItems().stream()
                .map(i -> OrderResponse.OrderItemDto.builder()
                        .orderItemId(i.getOrderItemId())
                        .productId(i.getProductId())
                        .title(i.getTitle())
                        .imageUrl(i.getImageUrl())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .weightKg(i.getWeightKg())
                        .unitPrice(i.getUnitPrice())
                        .originalPrice(i.getOriginalPrice())
                        .appliedTier(i.getAppliedTier())
                        .gstRate(i.getGstRate())
                        .lineTotal(i.getLineTotal())
                        .lineGst(i.getLineGst())
                        .sellerId(i.getSeller() != null ? i.getSeller().getSellerId() : null)
                        .build())
                .toList() : new ArrayList<>();

        String firstTitle = itemDtos.isEmpty() ? null : itemDtos.get(0).getTitle();
        String firstImage = itemDtos.isEmpty() ? null : itemDtos.get(0).getImageUrl();
        Store store = o.getStore();
        Customer customer = o.getCustomer();
        String recName = (o.getRecipientName() != null && !o.getRecipientName().isBlank())
                ? o.getRecipientName()
                : (customer != null ? customer.getName() : null);
        String recPhone = (o.getRecipientPhone() != null && !o.getRecipientPhone().isBlank())
                ? o.getRecipientPhone()
                : (customer != null ? customer.getPhone() : null);

        return OrderResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber(o.getOrderNumber())
                .orderGroupId(o.getOrderGroup() != null ? o.getOrderGroup().getId() : null)
                .groupOrderNumber(o.getOrderGroup() != null ? o.getOrderGroup().getGroupOrderNumber() : null)
                .customerId(customer != null ? customer.getCustomerId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .storeId(store != null ? store.getStoreId() : null)
                .storeName(store != null ? store.getName() : null)
                .storeSlug(store != null ? store.getSlug() : null)
                .fulfillmentStoreId(o.getFulfillmentStore() != null ? o.getFulfillmentStore().getStoreId() : null)
                .fulfillmentStoreName(o.getFulfillmentStore() != null ? o.getFulfillmentStore().getName() : null)
                .inventoryState(o.getInventoryState() != null ? o.getInventoryState().name() : null)
                .storeInvoiceNumber(o.getStoreInvoiceNumber())
                .commissionRate(o.getCommissionRate() != null ? o.getCommissionRate() : BigDecimal.ZERO)
                .commissionAmount(o.getCommissionAmount() != null ? o.getCommissionAmount() : BigDecimal.ZERO)
                .totalAmount(o.getTotalAmount())
                .totalWeightKg(o.getTotalWeightKg())
                .subtotal(o.getSubtotal())
                .discount(o.getDiscount())
                .taxableAmount(o.getTaxableAmount())
                .cgst(o.getCgst())
                .sgst(o.getSgst())
                .igst(o.getIgst())
                .totalGst(o.getTotalGst())
                .freightCharge(o.getFreightCharge())
                .craneUnloadingCharge(o.getCraneUnloadingCharge())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .orderStatus(o.getOrderStatus())
                .poNumber(o.getPoNumber())
                .deliveryLocation(o.getDeliveryLocation())
                .deliverySlot(o.getDeliverySlot())
                .deliveryInstructions(o.getDeliveryInstructions())
                .requiresCraneUnloading(o.isRequiresCraneUnloading())
                .orderForSomeoneElse(o.getOrderForSomeoneElse())
                .recipientName(o.getRecipientName())
                .recipientPhone(o.getRecipientPhone())
                .itemCount(itemDtos.size())
                .firstItemTitle(firstTitle)
                .firstItemImage(firstImage)
                .items(itemDtos)
                .createdAt(o.getCreatedAt())
                .estimatedDelivery(o.getEstimatedDelivery())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SellerOrderPageData getSellerOrders(Integer sellerId, String status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<Order> orderPage;
        if (sellerId != null) {
            orderPage = (status != null && !status.trim().isEmpty())
                    ? orderRepository.findSellerOrdersByStatus(sellerId, status.trim().toUpperCase(), pageable)
                    : orderRepository.findSellerOrders(sellerId, pageable);
        } else if (SecurityUtils.isAdmin()) {
            orderPage = (status != null && !status.trim().isEmpty())
                    ? orderRepository.findByOrderStatusIgnoreCaseOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable)
                    : orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            Integer currentSellerId = SecurityUtils.getCurrentSellerId()
                    .or(() -> {
                        try {
                            return Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    })
                    .orElseThrow(() -> new UnauthorizedException("Seller identity could not be resolved."));

            orderPage = (status != null && !status.trim().isEmpty())
                    ? orderRepository.findSellerOrdersByStatus(currentSellerId, status.trim().toUpperCase(), pageable)
                    : orderRepository.findSellerOrders(currentSellerId, pageable);
            sellerId = currentSellerId;
        }

        final Integer effectiveSellerId = sellerId;
        List<SellerOrderDto> dtoList = orderPage.getContent().stream()
                .map(o -> mapToSellerOrderDto(o, effectiveSellerId))
                .toList();

        return SellerOrderPageData.builder()
                .content(dtoList)
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .page(pageNumber)
                .limit(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SellerOrderDto getSellerOrderById(Integer id, Integer sellerId) {
        Order order = findOrder(id);
        if (!SecurityUtils.isAdmin()) {
            Integer effectiveSellerId = sellerId != null ? sellerId : SecurityUtils.getCurrentSellerId()
                    .or(() -> {
                        try {
                            return Optional.ofNullable(sellerContextUtil.getCurrentSellerId());
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    })
                    .orElseThrow(() -> new UnauthorizedException("Seller identity could not be resolved."));

            if (!isSellerAuthorizedForOrder(order, effectiveSellerId)) {
                throw new ForbiddenException("Access denied: You do not have permission to access Order #" + id);
            }
            sellerId = effectiveSellerId;
        }
        return mapToSellerOrderDto(order, sellerId);
    }

    private SellerOrderDto mapToSellerOrderDto(Order order, Integer sellerId) {
        if (order == null) return null;

        String formattedCreatedAt = null;
        if (order.getCreatedAt() != null) {
            formattedCreatedAt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(order.getCreatedAt().atZone(ZoneId.systemDefault()));
        }

        Customer customer = order.getCustomer();
        Address address = null;
        if (order.getAddressId() != null) {
            address = addressRepository.findById(order.getAddressId()).orElse(null);
        }

        String buyerName = (order.getRecipientName() != null && !order.getRecipientName().isBlank())
                ? order.getRecipientName().trim()
                : (customer != null && customer.getName() != null ? customer.getName().trim() : "Customer");
        String buyerPhone = (order.getRecipientPhone() != null && !order.getRecipientPhone().isBlank())
                ? order.getRecipientPhone().trim()
                : (customer != null && customer.getPhone() != null ? customer.getPhone().trim() : "");
        String buyerEmail = customer != null ? customer.getEmail() : null;
        String company = (address != null && address.getSiteName() != null && !address.getSiteName().isBlank())
                ? address.getSiteName().trim()
                : (buyerName + " Enterprises");

        SellerOrderDto.BuyerDto buyer = SellerOrderDto.BuyerDto.builder()
                .name(buyerName)
                .phone(buyerPhone)
                .email(buyerEmail)
                .company(company)
                .build();

        String completeAddr = order.getDeliveryLocation();
        String city = address != null ? address.getCity() : null;
        String state = address != null ? address.getState() : null;
        String pincode = address != null ? address.getPincode() : null;

        if (completeAddr == null && address != null) {
            completeAddr = java.util.stream.Stream.of(
                            address.getHouseFlatNo(), address.getAddressLine1(),
                            address.getAddressLine2(), address.getAreaLocality(),
                            address.getCity(), address.getState(), address.getPincode())
                    .filter(s -> s != null && !s.isBlank())
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        SellerOrderDto.DeliveryAddressDto deliveryAddress = SellerOrderDto.DeliveryAddressDto.builder()
                .completeAddress(completeAddr)
                .city(city)
                .state(state)
                .pincode(pincode)
                .build();

        List<SellerOrderDto.SellerOrderItemDto> itemDtos = new ArrayList<>();
        BigDecimal sellerSubtotal = BigDecimal.ZERO;
        BigDecimal sellerGst = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                Integer itemSellerId = oi.getSeller() != null ? oi.getSeller().getSellerId() : null;
                Product product = null;
                if (oi.getProductId() != null) {
                    product = productRepository.findById(oi.getProductId()).orElse(null);
                }
                if (itemSellerId == null && product != null && product.getSeller() != null) {
                    itemSellerId = product.getSeller().getSellerId();
                }

                boolean belongsToSeller = (sellerId == null) || (itemSellerId != null && itemSellerId.equals(sellerId))
                        || (order.getStore() != null && order.getStore().getSeller() != null && sellerId.equals(order.getStore().getSeller().getSellerId()))
                        || (order.getFulfillmentStore() != null && order.getFulfillmentStore().getSeller() != null && sellerId.equals(order.getFulfillmentStore().getSeller().getSellerId()));

                if (belongsToSeller) {
                    String sku = (product != null && product.getSku() != null && !product.getSku().isBlank())
                            ? product.getSku() : "sku" + oi.getProductId();
                    SellerOrderDto.SellerOrderItemDto itemDto = SellerOrderDto.SellerOrderItemDto.builder()
                            .id(oi.getOrderItemId())
                            .productId(oi.getProductId())
                            .name(oi.getTitle())
                            .sku(sku)
                            .quantity(oi.getQuantity())
                            .unit(oi.getUnit())
                            .price(oi.getUnitPrice())
                            .total(oi.getLineTotal())
                            .imageUrl(oi.getImageUrl())
                            .sellerId(itemSellerId)
                            .build();
                    itemDtos.add(itemDto);

                    if (oi.getLineTotal() != null) {
                        sellerSubtotal = sellerSubtotal.add(oi.getLineTotal());
                    }
                    if (oi.getLineGst() != null) {
                        sellerGst = sellerGst.add(oi.getLineGst());
                    }
                }
            }
        }

        BigDecimal subtotal = (sellerId != null && !itemDtos.isEmpty() && order.getItems() != null && itemDtos.size() < order.getItems().size())
                ? sellerSubtotal : (order.getSubtotal() != null ? order.getSubtotal() : sellerSubtotal);
        BigDecimal gstAmount = (sellerId != null && !itemDtos.isEmpty() && order.getItems() != null && itemDtos.size() < order.getItems().size())
                ? sellerGst : (order.getTotalGst() != null ? order.getTotalGst() : sellerGst);
        BigDecimal totalAmount = (sellerId != null && !itemDtos.isEmpty() && order.getItems() != null && itemDtos.size() < order.getItems().size())
                ? sellerSubtotal.add(sellerGst) : (order.getTotalAmount() != null ? order.getTotalAmount() : sellerSubtotal.add(sellerGst));

        return SellerOrderDto.builder()
                .id(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .createdAt(formattedCreatedAt)
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(totalAmount)
                .subtotal(subtotal)
                .gstAmount(gstAmount)
                .paymentMethod(order.getPaymentMethod())
                .buyer(buyer)
                .deliveryAddress(deliveryAddress)
                .items(itemDtos)
                .build();
    }
}
