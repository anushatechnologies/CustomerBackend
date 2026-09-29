package com.example.project.customer;

import com.example.project.customer.dto.DeliveryVerificationRequest;
import com.example.project.customer.dto.DeliveryVerificationResponse;
import com.example.project.customer.dto.OrderResponse;
import com.example.project.customer.dto.RiderOfferRespondRequest;
import com.example.project.customer.dto.RiderOfferResponse;
import com.example.project.customer.dto.SellerOrderAcceptRequest;
import com.example.project.customer.dto.SellerOrderRejectRequest;
import com.example.project.customer.entity.BusinessType;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.DeliveryRider;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.PayoutLedgerStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.RiderDispatchOffer;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerPayoutLedger;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.VehicleType;
import com.example.project.customer.repository.DeliveryRiderRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.RiderDispatchOfferRepository;
import com.example.project.customer.repository.SellerPayoutLedgerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.service.DeliveryVerificationServiceImpl;
import com.example.project.customer.service.OrderService;
import com.example.project.customer.service.RiderDispatchService;
import com.example.project.customer.service.RiderDispatchServiceImpl;
import com.example.project.customer.service.SellerOrderServiceImpl;
import com.example.project.customer.service.VehicleTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MultiSellerOrderDispatchFlowTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryRiderRepository deliveryRiderRepository;

    @Mock
    private RiderDispatchOfferRepository riderDispatchOfferRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerPayoutLedgerRepository sellerPayoutLedgerRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private VehicleTypeService vehicleTypeService;

    private RiderDispatchServiceImpl riderDispatchService;
    private DeliveryVerificationServiceImpl deliveryVerificationService;
    private SellerOrderServiceImpl sellerOrderService;

    private Seller seller;
    private Store store;
    private Customer customer;
    private Order order;
    private DeliveryRider rider1;
    private DeliveryRider rider2;
    private DeliveryRider rider3;

    @BeforeEach
    void setUp() {
        riderDispatchService = new RiderDispatchServiceImpl(
                orderRepository,
                deliveryRiderRepository,
                riderDispatchOfferRepository,
                vehicleTypeService
        );

        deliveryVerificationService = new DeliveryVerificationServiceImpl(
                orderRepository,
                deliveryRiderRepository,
                sellerPayoutLedgerRepository,
                orderService
        );

        sellerOrderService = new SellerOrderServiceImpl(
                orderRepository,
                storeRepository,
                productRepository,
                sellerPayoutLedgerRepository,
                riderDispatchService,
                orderService,
                vehicleTypeService
        );

        // 1. Seller & Store setup
        seller = Seller.builder()
                .sellerId(101)
                .name("Shree Balaji Building Materials")
                .companyName("Balaji Cement & Steel Trading")
                .businessType(BusinessType.WHOLESALER)
                .email("balaji@example.com")
                .phone("9848011223")
                .build();

        store = Store.builder()
                .storeId(5)
                .seller(seller)
                .name("Balaji Central Depot")
                .slug("balaji-depot")
                .status(StoreStatus.ACTIVE)
                .latitude(17.440081)
                .longitude(78.348915)
                .build();

        customer = Customer.builder()
                .customerId(42)
                .name("Vikram Reddy")
                .email("vikram@example.com")
                .phone("9876543210")
                .build();

        order = Order.builder()
                .orderId(1001)
                .orderNumber("ORD-20260918-XYZ123")
                .store(store)
                .customer(customer)
                .deliveryLocation("Plot 55, Gachibowli, Hyderabad")
                .totalAmount(BigDecimal.valueOf(1850.00))
                .paymentMethod("RAZORPAY")
                .paymentStatus("PAID")
                .orderStatus("PLACED")
                .checkpoints(new ArrayList<>())
                .items(new ArrayList<>())
                .build();

        // 2. Candidate Riders at various distances from store (17.440081, 78.348915)
        // Rider 1: Very close (~1.0 km)
        rider1 = DeliveryRider.builder()
                .id(1L)
                .name("Arjun Sharma")
                .phone("9000111222")
                .vehicleType("TWO_WHEELER")
                .vehicleNumber("TS 07 EA 1234")
                .currentLatitude(17.4450)
                .currentLongitude(78.3520)
                .isOnline(true)
                .isAvailable(true)
                .build();

        // Rider 2: Medium distance (~3.5 km)
        rider2 = DeliveryRider.builder()
                .id(2L)
                .name("Bala Krishna")
                .phone("9000333444")
                .vehicleType("TWO_WHEELER")
                .vehicleNumber("TS 08 BC 5678")
                .currentLatitude(17.4650)
                .currentLongitude(78.3650)
                .isOnline(true)
                .isAvailable(true)
                .build();

        // Rider 3: Far (~8.0 km)
        rider3 = DeliveryRider.builder()
                .id(3L)
                .name("Chaitanya Varma")
                .phone("9000555666")
                .vehicleType("TWO_WHEELER")
                .vehicleNumber("TS 09 XY 9999")
                .currentLatitude(17.5000)
                .currentLongitude(78.4000)
                .isOnline(true)
                .isAvailable(true)
                .build();

        when(orderRepository.findById(1001)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        when(orderService.getOrderById(1001)).thenAnswer(inv ->
                OrderResponse.builder()
                        .orderId(order.getOrderId())
                        .orderNumber(order.getOrderNumber())
                        .orderStatus(order.getOrderStatus())
                        .deliveryOtp(order.getDeliveryOtp())
                        .selectedVehicleType(order.getSelectedVehicleType())
                        .sellerAcceptedAt(order.getSellerAcceptedAt())
                        .riderId(order.getRiderId())
                        .riderName(order.getRiderName())
                        .riderPhone(order.getRiderPhone())
                        .totalAmount(order.getTotalAmount())
                        .paymentStatus(order.getPaymentStatus())
                        .deliveredAt(order.getDeliveredAt())
                        .build()
        );

        VehicleType twoWheeler = VehicleType.builder()
                .code("TWO_WHEELER")
                .name("Two Wheeler / Bike")
                .baseFare(BigDecimal.valueOf(35.00))
                .baseDistanceKm(2.0)
                .perKmRate(BigDecimal.valueOf(10.00))
                .minimumFare(BigDecimal.valueOf(35.00))
                .maxWeightKg(BigDecimal.valueOf(20.0))
                .active(true)
                .build();

        VehicleType threeWheeler = VehicleType.builder()
                .code("THREE_WHEELER")
                .name("Three Wheeler / Auto")
                .baseFare(BigDecimal.valueOf(80.00))
                .baseDistanceKm(2.0)
                .perKmRate(BigDecimal.valueOf(18.00))
                .minimumFare(BigDecimal.valueOf(80.00))
                .maxWeightKg(BigDecimal.valueOf(300.0))
                .active(true)
                .build();

        when(vehicleTypeService.findEntityByCode(eq("TWO_WHEELER"))).thenReturn(twoWheeler);
        when(vehicleTypeService.findEntityByCode(eq("THREE_WHEELER"))).thenReturn(threeWheeler);
        when(vehicleTypeService.calculateFare(any(), anyDouble())).thenAnswer(inv -> {
            VehicleType vt = inv.getArgument(0);
            if (vt == null) return BigDecimal.valueOf(45.00);
            double dist = inv.getArgument(1);
            if (dist <= vt.getBaseDistanceKm()) {
                return vt.getBaseFare();
            }
            BigDecimal extra = BigDecimal.valueOf(dist - vt.getBaseDistanceKm()).multiply(vt.getPerKmRate());
            return vt.getBaseFare().add(extra);
        });
    }

    @Test
    @DisplayName("Phase 1: Seller accepts order -> 6-Digit Delivery OTP is generated and status becomes ACCEPTED_BY_SELLER")
    void testSellerAcceptanceGeneratesOtpAndTriggersDispatch() {
        when(deliveryRiderRepository.findAvailableOnlineRidersWithCoordinates())
                .thenReturn(List.of(rider1, rider2, rider3));
        when(riderDispatchOfferRepository.findByOrder_OrderIdOrderBySequenceIndexAsc(1001))
                .thenReturn(new ArrayList<>());
        when(riderDispatchOfferRepository.save(any(RiderDispatchOffer.class)))
                .thenAnswer(inv -> {
                    RiderDispatchOffer o = inv.getArgument(0);
                    o.setId(501L);
                    return o;
                });

        OrderResponse acceptedOrder = sellerOrderService.acceptOrder(seller.getSellerId(), order.getOrderId());

        // Verify order state
        assertThat(order.getOrderStatus()).isEqualTo("ACCEPTED_BY_SELLER");
        assertThat(order.getDeliveryOtp()).isNotNull();
        assertThat(order.getDeliveryOtp()).matches("\\d{6}"); // 6-digit numeric OTP
        assertThat(order.getDeliveryOtpAttempts()).isEqualTo(0);
        assertThat(order.getSellerAcceptedAt()).isNotNull();

        // Customer sees OTP in OrderResponse
        assertThat(acceptedOrder.getDeliveryOtp()).isEqualTo(order.getDeliveryOtp());
        assertThat(acceptedOrder.getOrderStatus()).isEqualTo("ACCEPTED_BY_SELLER");

        // Verify tracking checkpoint added
        assertThat(order.getCheckpoints()).isNotEmpty();
        assertThat(order.getCheckpoints().get(0).getStatus()).isEqualTo("ACCEPTED_BY_SELLER");
    }

    @Test
    @DisplayName("Phase 2: Sequential one-by-one rider dispatch -> Rider 1 rejects -> Cascades to Rider 2 -> Rider 2 accepts")
    void testSequentialRiderDispatchOneByOneWaterfall() {
        order.setOrderStatus("ACCEPTED_BY_SELLER");
        order.setDeliveryOtp("729415");

        // 1. Available riders queried
        when(deliveryRiderRepository.findAvailableOnlineRidersWithCoordinates())
                .thenReturn(List.of(rider3, rider1, rider2)); // Unsorted input

        List<RiderDispatchOffer> existingOffers = new ArrayList<>();
        when(riderDispatchOfferRepository.findByOrder_OrderIdOrderBySequenceIndexAsc(1001))
                .thenReturn(existingOffers);

        when(riderDispatchOfferRepository.save(any(RiderDispatchOffer.class)))
                .thenAnswer(inv -> {
                    RiderDispatchOffer o = inv.getArgument(0);
                    if (o.getId() == null) {
                        o.setId((long) (existingOffers.size() + 1));
                        existingOffers.add(o);
                    } else if (!existingOffers.contains(o)) {
                        existingOffers.add(o);
                    }
                    return o;
                });

        // 2. Initial dispatch starts -> Must offer closest rider first (Rider 1: ~1 km)
        boolean initiated = riderDispatchService.offerNextRider(1001, 1);
        assertThat(initiated).isTrue();
        assertThat(existingOffers).hasSize(1);

        RiderDispatchOffer offer1 = existingOffers.get(0);
        assertThat(offer1.getRider().getId()).isEqualTo(rider1.getId());
        assertThat(offer1.getSequenceIndex()).isEqualTo(1);
        assertThat(offer1.getStatus()).isEqualTo("OFFERED");

        // 3. Rider 1 rejects the offer
        when(riderDispatchOfferRepository.findById(offer1.getId())).thenReturn(Optional.of(offer1));
        RiderOfferRespondRequest rejectReq = RiderOfferRespondRequest.builder()
                .action("REJECT")
                .reason("Too far from current drop")
                .build();

        boolean rejectResponded = riderDispatchService.respondToOffer(rider1.getId(), offer1.getId(), rejectReq);
        assertThat(rejectResponded).isTrue();
        assertThat(offer1.getStatus()).isEqualTo("REJECTED");

        // Cascades automatically to next candidate: Rider 2 (next closest)
        assertThat(existingOffers).hasSize(2);
        RiderDispatchOffer offer2 = existingOffers.get(1);
        assertThat(offer2.getRider().getId()).isEqualTo(rider2.getId());
        assertThat(offer2.getSequenceIndex()).isEqualTo(2);
        assertThat(offer2.getStatus()).isEqualTo("OFFERED");

        // 4. Rider 2 accepts the offer
        when(riderDispatchOfferRepository.findById(offer2.getId())).thenReturn(Optional.of(offer2));
        when(orderRepository.atomicAssignRiderToOrder(
                eq(1001), eq(rider2.getId()), eq(rider2.getName()), eq(rider2.getPhone()),
                eq(rider2.getVehicleNumber()), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    order.setRiderId(rider2.getId());
                    order.setRiderName(rider2.getName());
                    order.setRiderPhone(rider2.getPhone());
                    order.setOrderStatus("RIDER_ASSIGNED");
                    return 1; // 1 row updated (atomic success)
                });

        RiderOfferRespondRequest acceptReq = RiderOfferRespondRequest.builder()
                .action("ACCEPT")
                .build();

        boolean acceptResponded = riderDispatchService.respondToOffer(rider2.getId(), offer2.getId(), acceptReq);
        assertThat(acceptResponded).isTrue();
        assertThat(offer2.getStatus()).isEqualTo("ACCEPTED");

        // Order is now locked to Rider 2
        assertThat(order.getRiderId()).isEqualTo(rider2.getId());
        assertThat(order.getOrderStatus()).isEqualTo("RIDER_ASSIGNED");

        // Verify competing offers are cancelled
        verify(riderDispatchOfferRepository).cancelPendingOffersForOrder(eq(1001), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Phase 3 (Prepaid): Pickup -> Arrived -> Invalid OTP attempt -> Correct OTP -> DELIVERED & Payout released")
    void testDeliveryPickupAndDoorstepVerificationPrepaid() {
        order.setOrderStatus("RIDER_ASSIGNED");
        order.setRiderId(rider2.getId());
        order.setDriverName(rider2.getName());
        order.setDeliveryOtp("843219");
        order.setPaymentMethod("RAZORPAY");
        order.setPaymentStatus("PAID");

        SellerPayoutLedger ledger = SellerPayoutLedger.builder()
                .id(99)
                .order(order)
                .status(PayoutLedgerStatus.ON_HOLD)
                .build();
        when(sellerPayoutLedgerRepository.findByOrder_OrderId(1001)).thenReturn(Optional.of(ledger));
        when(deliveryRiderRepository.findById(rider2.getId())).thenReturn(Optional.of(rider2));

        // 1. Rider confirms pickup
        OrderResponse pickedUpResponse = deliveryVerificationService.confirmPickup(rider2.getId(), 1001);
        assertThat(order.getOrderStatus()).isEqualTo("IN_TRANSIT");
        assertThat(order.getPickedUpAt()).isNotNull();

        // 2. Rider marks arrived at customer
        deliveryVerificationService.markArrivedAtCustomer(rider2.getId(), 1001);
        assertThat(order.getOrderStatus()).isEqualTo("OUT_FOR_DELIVERY");

        // 3. Rider enters WRONG OTP (e.g. 111111)
        DeliveryVerificationRequest wrongOtpRequest = DeliveryVerificationRequest.builder()
                .otp("111111")
                .build();

        assertThatThrownBy(() -> deliveryVerificationService.verifyAndCompleteDelivery(rider2.getId(), 1001, wrongOtpRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Delivery OTP. 2 attempt(s) remaining.");

        assertThat(order.getDeliveryOtpAttempts()).isEqualTo(1);
        assertThat(order.getOrderStatus()).isEqualTo("OUT_FOR_DELIVERY"); // Not delivered yet

        // 4. Rider enters CORRECT OTP (843219)
        DeliveryVerificationRequest correctOtpRequest = DeliveryVerificationRequest.builder()
                .otp("843219")
                .build();

        DeliveryVerificationResponse deliveryResponse = deliveryVerificationService.verifyAndCompleteDelivery(
                rider2.getId(), 1001, correctOtpRequest);

        assertThat(deliveryResponse.isSuccess()).isTrue();
        assertThat(deliveryResponse.getOrderStatus()).isEqualTo("DELIVERED");
        assertThat(order.getOrderStatus()).isEqualTo("DELIVERED");
        assertThat(order.getDeliveredAt()).isNotNull();
        assertThat(order.getDeliveryOtpVerifiedAt()).isNotNull();

        // Rider is freed
        assertThat(rider2.isAvailable()).isTrue();
        assertThat(rider2.getActiveOrderId()).isNull();

        // Payout ledger moved to PENDING for settlement
        assertThat(ledger.getStatus()).isEqualTo(PayoutLedgerStatus.PENDING);
        assertThat(ledger.getSettlementDate()).isNotNull();
    }

    @Test
    @DisplayName("Phase 3 (COD): Delivery verification requires cash collection verification before marking DELIVERED")
    void testDeliveryPickupAndDoorstepVerificationCod() {
        order.setOrderStatus("OUT_FOR_DELIVERY");
        order.setRiderId(rider2.getId());
        order.setDriverName(rider2.getName());
        order.setDeliveryOtp("334455");
        order.setPaymentMethod("COD");
        order.setPaymentStatus("PENDING"); // Unpaid
        order.setTotalAmount(BigDecimal.valueOf(1850.00));

        when(deliveryRiderRepository.findById(rider2.getId())).thenReturn(Optional.of(rider2));
        when(sellerPayoutLedgerRepository.findByOrder_OrderId(1001)).thenReturn(Optional.empty());

        // 1. Correct OTP but missing/insufficient cash collection -> Throws exception
        DeliveryVerificationRequest insufficientCodRequest = DeliveryVerificationRequest.builder()
                .otp("334455")
                .codAmountCollected(BigDecimal.valueOf(1000.00)) // Less than 1850
                .build();

        assertThatThrownBy(() -> deliveryVerificationService.verifyAndCompleteDelivery(rider2.getId(), 1001, insufficientCodRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incomplete payment collected");

        assertThat(order.getOrderStatus()).isEqualTo("OUT_FOR_DELIVERY");

        // 2. Correct OTP and full cash collection -> Completes delivery & marks payment PAID
        DeliveryVerificationRequest fullCodRequest = DeliveryVerificationRequest.builder()
                .otp("334455")
                .codAmountCollected(BigDecimal.valueOf(1850.00))
                .build();

        DeliveryVerificationResponse response = deliveryVerificationService.verifyAndCompleteDelivery(
                rider2.getId(), 1001, fullCodRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("DELIVERED");
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getCodAmountCollected()).isEqualByComparingTo("1850.00");
    }

    @Test
    @DisplayName("Seller Rejection: Rejects order -> Restores inventory and marks ledger REVERSED")
    void testSellerRejectionRestoresStockAndReversesLedger() {
        Product p = Product.builder()
                .productId(201)
                .title("Birla A1 Cement")
                .stockQty(10)
                .build();

        OrderItem item = OrderItem.builder()
                .productId(201)
                .quantity(5)
                .build();
        order.getItems().add(item);

        SellerPayoutLedger ledger = SellerPayoutLedger.builder()
                .id(105)
                .order(order)
                .status(PayoutLedgerStatus.PENDING)
                .build();

        when(productRepository.findById(201)).thenReturn(Optional.of(p));
        when(sellerPayoutLedgerRepository.findByOrder_OrderId(1001)).thenReturn(Optional.of(ledger));

        SellerOrderRejectRequest rejectRequest = SellerOrderRejectRequest.builder()
                .reason("Inventory depleted by wholesale buyer")
                .build();

        OrderResponse response = sellerOrderService.rejectOrder(seller.getSellerId(), 1001, rejectRequest);

        assertThat(order.getOrderStatus()).isEqualTo("REJECTED_BY_SELLER");
        assertThat(order.getSellerRejectionReason()).isEqualTo("Inventory depleted by wholesale buyer");
        assertThat(order.getPaymentStatus()).isEqualTo("REFUND_PENDING");

        // Stock restored: 10 + 5 = 15
        assertThat(p.getStockQty()).isEqualTo(15);

        // Ledger reversed
        assertThat(ledger.getStatus()).isEqualTo(PayoutLedgerStatus.REVERSED);
    }

    @Test
    @DisplayName("Vehicle Type Feature: Seller selects THREE_WHEELER -> Dispatch ONLY notifies matching THREE_WHEELER riders")
    void testSellerSelectsVehicleTypeAndDispatchOffersOnlyMatchingRiders() {
        // Change rider3 to THREE_WHEELER
        rider3.setVehicleType("THREE_WHEELER");

        when(deliveryRiderRepository.findAvailableOnlineRidersWithCoordinates())
                .thenReturn(List.of(rider1, rider2, rider3)); // rider1 & rider2 are TWO_WHEELER, rider3 is THREE_WHEELER

        List<RiderDispatchOffer> existingOffers = new ArrayList<>();
        when(riderDispatchOfferRepository.findByOrder_OrderIdOrderBySequenceIndexAsc(1001))
                .thenReturn(existingOffers);

        when(riderDispatchOfferRepository.save(any(RiderDispatchOffer.class)))
                .thenAnswer(inv -> {
                    RiderDispatchOffer o = inv.getArgument(0);
                    if (o.getId() == null) {
                        o.setId((long) (existingOffers.size() + 1));
                        existingOffers.add(o);
                    }
                    return o;
                });

        // 1. Seller accepts order and selects THREE_WHEELER based on load
        SellerOrderAcceptRequest acceptRequest = SellerOrderAcceptRequest.builder()
                .vehicleTypeCode("THREE_WHEELER")
                .build();

        OrderResponse response = sellerOrderService.acceptOrder(seller.getSellerId(), order.getOrderId(), acceptRequest);

        assertThat(response.getOrderStatus()).isEqualTo("ACCEPTED_BY_SELLER");
        assertThat(order.getSelectedVehicleType()).isEqualTo("THREE_WHEELER");

        // 2. Verify ONLY Rider 3 (THREE_WHEELER) received the offer, rider 1 & 2 (TWO_WHEELER) were filtered out
        assertThat(existingOffers).hasSize(1);
        RiderDispatchOffer createdOffer = existingOffers.get(0);
        assertThat(createdOffer.getRider().getId()).isEqualTo(rider3.getId());
        assertThat(createdOffer.getRider().getVehicleType()).isEqualTo("THREE_WHEELER");
        assertThat(createdOffer.getStatus()).isEqualTo("OFFERED");
    }

    @Test
    @DisplayName("Vehicle Type Fare: Rider dispatch offer calculates fare dynamically from Admin Vehicle Type fare options")
    void testRiderOfferCalculatesFareFromVehicleTypeOptions() {
        rider3.setVehicleType("THREE_WHEELER");
        order.setSelectedVehicleType("THREE_WHEELER");
        order.setOrderStatus("ACCEPTED_BY_SELLER");

        when(deliveryRiderRepository.findAvailableOnlineRidersWithCoordinates())
                .thenReturn(List.of(rider3));

        List<RiderDispatchOffer> existingOffers = new ArrayList<>();
        when(riderDispatchOfferRepository.findByOrder_OrderIdOrderBySequenceIndexAsc(1001))
                .thenReturn(existingOffers);

        when(riderDispatchOfferRepository.save(any(RiderDispatchOffer.class)))
                .thenAnswer(inv -> {
                    RiderDispatchOffer o = inv.getArgument(0);
                    o.setId(99L);
                    existingOffers.add(o);
                    return o;
                });

        boolean dispatched = riderDispatchService.offerNextRider(1001, 1);
        assertThat(dispatched).isTrue();

        assertThat(existingOffers).hasSize(1);
        RiderDispatchOffer offer = existingOffers.get(0);
        // Base fare 80 + (distance - 2) * 18 > 80.00
        assertThat(offer.getOfferedFare()).isGreaterThan(BigDecimal.valueOf(80.00));
        assertThat(offer.getRider().getVehicleType()).isEqualTo("THREE_WHEELER");
    }
}
