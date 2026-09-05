package com.example.project.customer.config;

import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Banner;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.ProductReview;
import com.example.project.customer.entity.Quotation;
import com.example.project.customer.entity.ReviewStatus;
import com.example.project.customer.entity.Rfq;
import com.example.project.customer.entity.RfqQuestion;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.entity.UserProfile;
import com.example.project.customer.entity.VendorInfo;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.BannerRepository;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.ProductReviewRepository;
import com.example.project.customer.repository.QuotationRepository;
import com.example.project.customer.repository.RfqQuestionRepository;
import com.example.project.customer.repository.RfqRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import com.example.project.customer.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BannerRepository bannerRepository;
    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;
    private final RfqRepository rfqRepository;
    private final QuotationRepository quotationRepository;
    private final RfqQuestionRepository rfqQuestionRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductReviewRepository productReviewRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.example.project.customer.repository.WalletRepository walletRepository;
    private final com.example.project.customer.repository.WalletTransactionRepository walletTransactionRepository;
    private final com.example.project.customer.repository.RewardVoucherRepository rewardVoucherRepository;
    private final com.example.project.customer.repository.RentalEquipmentRepository rentalEquipmentRepository;
    private final com.example.project.customer.repository.PurchaseOrderRepository purchaseOrderRepository;
    private final com.example.project.customer.repository.PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final com.example.project.customer.repository.ConversationRepository conversationRepository;
    private final com.example.project.customer.repository.ChatMessageRepository chatMessageRepository;
    private final com.example.project.customer.repository.BlogArticleRepository blogArticleRepository;
    private final com.example.project.customer.repository.NewsItemRepository newsItemRepository;
    private final com.example.project.customer.repository.SupportTicketRepository supportTicketRepository;
    private final com.example.project.customer.repository.TicketMessageRepository ticketMessageRepository;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE seller_documents MODIFY COLUMN document_type VARCHAR(50) NOT NULL");
            jdbcTemplate.execute("ALTER TABLE seller_documents MODIFY COLUMN title VARCHAR(150) NULL DEFAULT ''");
        } catch (Exception ignored) {
        }
        try {
            initUserProfile();
            initAddresses();
            initCatalogAndBanners();
            initSampleRfq();
            initExtendedModules();
            log.info("HINCH MART database successfully initialized with rich B2B catalog and seed data.");
        } catch (Exception e) {
            log.warn("Data initialization skipped or already present: {}", e.getMessage(), e);
        }
    }

    private void initUserProfile() {
        if (userProfileRepository.count() == 0) {
            UserProfile user = UserProfile.builder()
                    .id(101)
                    .fullName("Rajesh Sharma")
                    .phone("9876543210")
                    .email("rajesh@apexbldrs.com")
                    .role("BUYER")
                    .tier("GOLD")
                    .profileComplete(true)
                    .companyName("Apex Infra Projects Pvt Ltd")
                    .gstNumber("36AAACT2727Q1ZW")
                    .panNumber("AAACT2727Q")
                    .businessType("General Contractor")
                    .gstVerified(true)
                    .creditLimit(BigDecimal.valueOf(5000000.0))
                    .availableCredit(BigDecimal.valueOf(3250000.0))
                    .build();
            userProfileRepository.save(user);
        }
        if (customerRepository.count() == 0) {
            Customer customer1 = Customer.builder()
                    .customerId(101)
                    .name("Rajesh Sharma")
                    .email("rajesh@apexbldrs.com")
                    .phone("9876543210")
                    .build();
            Customer customer2 = Customer.builder()
                    .customerId(102)
                    .name("Ananya Reddy")
                    .email("ananya@infrahyderabad.in")
                    .phone("9849012345")
                    .build();
            customerRepository.saveAll(List.of(customer1, customer2));
        }
    }

    private void initAddresses() {
        if (addressRepository.count() == 0) {
            Address site1 = Address.builder()
                    .siteName("Tower B Project Site")
                    .recipientName("Site Eng. Vikram Reddy")
                    .phone("9849112233")
                    .addressLine1("Plot 42, Financial District")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("500032")
                    .landmark("Opp. WaveRock SEZ")
                    .isDefault(true)
                    .hasHeavyVehicleAccess(true)
                    .build();

            Address site2 = Address.builder()
                    .siteName("Highway Expansion Yard - Outer Ring Road")
                    .recipientName("Store Mgr. Anil Kumar")
                    .phone("9849223344")
                    .addressLine1("Survey 108, Exit 11 ORR, Pedda Amberpet")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("501505")
                    .landmark("Near Toll Plaza Gate 3")
                    .isDefault(false)
                    .hasHeavyVehicleAccess(true)
                    .build();

            Address site3 = Address.builder()
                    .siteName("Gachibowli Metro Depot Project")
                    .recipientName("Procurement Officer Ramesh")
                    .phone("9849334455")
                    .addressLine1("Metro Pillar 140, Old Mumbai Highway")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("500081")
                    .landmark("Behind Bio-Diversity Park")
                    .isDefault(false)
                    .hasHeavyVehicleAccess(true)
                    .build();

            Address site4 = Address.builder()
                    .siteName("Commercial Warehouse 4")
                    .recipientName("Logistics Lead Suresh")
                    .phone("9849445566")
                    .addressLine1("Shed 12, IDA Nacharam Industrial Area")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("500076")
                    .landmark("Road No. 5")
                    .isDefault(false)
                    .hasHeavyVehicleAccess(true)
                    .build();

            addressRepository.saveAll(List.of(site1, site2, site3, site4));
        }
    }

    private void initCatalogAndBanners() {
        if (brandRepository.count() == 0) {
            Category civil = categoryRepository.findBySlugIgnoreCase("civil-structural")
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name("Civil & Structural")
                            .slug("civil-structural")
                            .imageUrl("https://cdn.hinchmart.com/categories/civil_structural.jpg")
                            .active(true)
                            .sortOrder(1)
                            .build()));

            Category electrical = categoryRepository.findBySlugIgnoreCase("electrical-cables")
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name("Electrical & Cables")
                            .slug("electrical-cables")
                            .imageUrl("https://cdn.hinchmart.com/categories/electrical.jpg")
                            .active(true)
                            .sortOrder(2)
                            .build()));

            Category plumbing = categoryRepository.findBySlugIgnoreCase("plumbing-sanitary")
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name("Plumbing & Sanitary")
                            .slug("plumbing-sanitary")
                            .imageUrl("https://cdn.hinchmart.com/categories/plumbing.jpg")
                            .active(true)
                            .sortOrder(3)
                            .build()));

            Category paints = categoryRepository.findBySlugIgnoreCase("paints-finishes")
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name("Paints & Finishes")
                            .slug("paints-finishes")
                            .imageUrl("https://cdn.hinchmart.com/categories/paints.jpg")
                            .active(true)
                            .sortOrder(4)
                            .build()));

            // Subcategories
            Subcategory tmt = subcategoryRepository.findBySlugIgnoreCase("tmt-steel-rebars")
                    .orElseGet(() -> subcategoryRepository.save(Subcategory.builder()
                            .category(civil)
                            .name("TMT Steel & Rebars")
                            .slug("tmt-steel-rebars")
                            .imageUrl("https://cdn.hinchmart.com/subcategories/tmt_steel.jpg")
                            .active(true)
                            .sortOrder(1)
                            .build()));

            Subcategory cement = subcategoryRepository.findBySlugIgnoreCase("cement-rmc")
                    .orElseGet(() -> subcategoryRepository.save(Subcategory.builder()
                            .category(civil)
                            .name("Cement & RMC")
                            .slug("cement-rmc")
                            .imageUrl("https://cdn.hinchmart.com/subcategories/cement.jpg")
                            .active(true)
                            .sortOrder(2)
                            .build()));

            Subcategory cables = subcategoryRepository.findBySlugIgnoreCase("armoured-cables")
                    .orElseGet(() -> subcategoryRepository.save(Subcategory.builder()
                            .category(electrical)
                            .name("Armoured XLPE Cables")
                            .slug("armoured-cables")
                            .imageUrl("https://cdn.hinchmart.com/subcategories/armoured_cables.jpg")
                            .active(true)
                            .sortOrder(1)
                            .build()));

            Subcategory pipes = subcategoryRepository.findBySlugIgnoreCase("cpvc-pipes")
                    .orElseGet(() -> subcategoryRepository.save(Subcategory.builder()
                            .category(plumbing)
                            .name("CPVC & UPVC Pipes")
                            .slug("cpvc-pipes")
                            .imageUrl("https://cdn.hinchmart.com/subcategories/pipes.jpg")
                            .active(true)
                            .sortOrder(1)
                            .build()));

            // Brands
            Brand tataTiscon = brandRepository.save(Brand.builder()
                    .subcategory(tmt)
                    .name("Tata Tiscon")
                    .slug("tata-tiscon")
                    .imageUrl("https://cdn.hinchmart.com/brands/tata_tiscon.png")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Brand jswSteel = brandRepository.save(Brand.builder()
                    .subcategory(tmt)
                    .name("JSW Neosteel")
                    .slug("jsw-neosteel")
                    .imageUrl("https://cdn.hinchmart.com/brands/jsw_steel.png")
                    .active(true)
                    .sortOrder(2)
                    .build());

            Brand jindal = brandRepository.save(Brand.builder()
                    .subcategory(tmt)
                    .name("Jindal Panther")
                    .slug("jindal-panther")
                    .imageUrl("https://cdn.hinchmart.com/brands/jindal_panther.png")
                    .active(true)
                    .sortOrder(3)
                    .build());

            Brand ultratech = brandRepository.save(Brand.builder()
                    .subcategory(cement)
                    .name("UltraTech")
                    .slug("ultratech")
                    .imageUrl("https://cdn.hinchmart.com/brands/ultratech.png")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Brand acc = brandRepository.save(Brand.builder()
                    .subcategory(cement)
                    .name("ACC Cement")
                    .slug("acc-cement")
                    .imageUrl("https://cdn.hinchmart.com/brands/acc_cement.png")
                    .active(true)
                    .sortOrder(2)
                    .build());

            Brand ambuja = brandRepository.save(Brand.builder()
                    .subcategory(cement)
                    .name("Ambuja Cement")
                    .slug("ambuja-cement")
                    .imageUrl("https://cdn.hinchmart.com/brands/ambuja_cement.png")
                    .active(true)
                    .sortOrder(3)
                    .build());

            Brand polycab = brandRepository.save(Brand.builder()
                    .subcategory(cables)
                    .name("Polycab")
                    .slug("polycab")
                    .imageUrl("https://cdn.hinchmart.com/brands/polycab.png")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Brand havells = brandRepository.save(Brand.builder()
                    .subcategory(cables)
                    .name("Havells")
                    .slug("havells")
                    .imageUrl("https://cdn.hinchmart.com/brands/havells.png")
                    .active(true)
                    .sortOrder(2)
                    .build());

            Brand kei = brandRepository.save(Brand.builder()
                    .subcategory(cables)
                    .name("KEI Wires & Cables")
                    .slug("kei-wires-cables")
                    .imageUrl("https://cdn.hinchmart.com/brands/kei.png")
                    .active(true)
                    .sortOrder(3)
                    .build());

            Brand astral = brandRepository.save(Brand.builder()
                    .subcategory(pipes)
                    .name("Astral")
                    .slug("astral")
                    .imageUrl("https://cdn.hinchmart.com/brands/astral.png")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Brand ashirvad = brandRepository.save(Brand.builder()
                    .subcategory(pipes)
                    .name("Ashirvad Pipes")
                    .slug("ashirvad-pipes")
                    .imageUrl("https://cdn.hinchmart.com/brands/ashirvad.png")
                    .active(true)
                    .sortOrder(2)
                    .build());

            Brand finolex = brandRepository.save(Brand.builder()
                    .subcategory(pipes)
                    .name("Finolex")
                    .slug("finolex")
                    .imageUrl("https://cdn.hinchmart.com/brands/finolex.png")
                    .active(true)
                    .sortOrder(3)
                    .build());

            // Products
            List<BulkPricingTier> tmtTiers = List.of(
                    BulkPricingTier.builder().tierId(1).minQty(5).maxQty(19).price(BigDecimal.valueOf(54200.0)).discountPercentage(8.1).build(),
                    BulkPricingTier.builder().tierId(2).minQty(20).maxQty(49).price(BigDecimal.valueOf(52800.0)).discountPercentage(10.5).build(),
                    BulkPricingTier.builder().tierId(3).minQty(50).maxQty(null).price(BigDecimal.valueOf(51200.0)).discountPercentage(13.2).build()
            );

            Map<String, String> tmtSpecs = new LinkedHashMap<>();
            tmtSpecs.put("Standard", "IS 1786:2008");
            tmtSpecs.put("Grade", "Fe 550D");
            tmtSpecs.put("Diameter", "12 mm");
            tmtSpecs.put("Yield Strength", "550 N/mm²");
            tmtSpecs.put("Manufacturer Test Certificate (MTC)", "Included per batch");

            Product p1 = Product.builder()
                    .brand(tataTiscon)
                    .title("TMT Steel Rebars Fe 550D (12mm)")
                    .slug("tmt-steel-rebars-fe-550d-12mm")
                    .sku("STL-TMT-12-FE550D")
                    .description("High-ductility primary steel rebars conforming to IS 1786 standards.")
                    .imageUrl("https://cdn.hinchmart.com/products/tmt_steel_12mm.jpg")
                    .images(List.of(
                            "https://cdn.hinchmart.com/products/tmt_steel_12mm_1.jpg",
                            "https://cdn.hinchmart.com/products/tmt_steel_12mm_2.jpg",
                            "https://cdn.hinchmart.com/products/tmt_steel_12mm_3.jpg"
                    ))
                    .price(BigDecimal.valueOf(54200.0))
                    .mrp(BigDecimal.valueOf(59000.0))
                    .unit("MT")
                    .moq(5)
                    .stockQty(500)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .is24HourDelivery(true)
                    .rating(0.0)
                    .reviewCount(0)
                    .gstRate(BigDecimal.valueOf(18.0))
                    .hsnCode("7214")
                    .specifications(tmtSpecs)
                    .bulkPricingTiers(tmtTiers)
                    .vendor(VendorInfo.builder()
                            .vendorId(12)
                            .companyName("Tata Steel Distribution Yard")
                            .city("Hyderabad")
                            .isVerified(true)
                            .rating(4.9)
                            .build())
                    .build();

            List<BulkPricingTier> cementTiers = List.of(
                    BulkPricingTier.builder().tierId(1).minQty(100).maxQty(499).price(BigDecimal.valueOf(380.0)).discountPercentage(9.5).build(),
                    BulkPricingTier.builder().tierId(2).minQty(500).maxQty(null).price(BigDecimal.valueOf(360.0)).discountPercentage(14.3).build()
            );

            Map<String, String> cementSpecs = new LinkedHashMap<>();
            cementSpecs.put("Grade", "PPC Conforming to IS 1489");
            cementSpecs.put("Bag Weight", "50 kg HDPE Bag");
            cementSpecs.put("Initial Setting Time", "120 mins");

            Product p2 = Product.builder()
                    .brand(ultratech)
                    .title("UltraTech Super Cement PPC (50kg Bag)")
                    .slug("ultratech-super-cement-ppc-50kg")
                    .sku("CMT-PPC-50KG-UT")
                    .description("Premium Portland Pozzolana Cement for high durability concrete construction.")
                    .imageUrl("https://cdn.hinchmart.com/products/ultratech_cement.jpg")
                    .images(List.of("https://cdn.hinchmart.com/products/ultratech_cement_1.jpg"))
                    .price(BigDecimal.valueOf(380.0))
                    .mrp(BigDecimal.valueOf(420.0))
                    .unit("Bags")
                    .moq(100)
                    .stockQty(2500)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .is24HourDelivery(true)
                    .rating(0.0)
                    .reviewCount(0)
                    .gstRate(BigDecimal.valueOf(28.0))
                    .hsnCode("2523")
                    .specifications(cementSpecs)
                    .bulkPricingTiers(cementTiers)
                    .vendor(VendorInfo.builder()
                            .vendorId(18)
                            .companyName("UltraTech Authorized Depot")
                            .city("Hyderabad")
                            .isVerified(true)
                            .rating(4.8)
                            .build())
                    .build();

            List<BulkPricingTier> cableTiers = List.of(
                    BulkPricingTier.builder().tierId(1).minQty(100).maxQty(499).price(BigDecimal.valueOf(215.0)).discountPercentage(17.3).build(),
                    BulkPricingTier.builder().tierId(2).minQty(500).maxQty(null).price(BigDecimal.valueOf(195.0)).discountPercentage(25.0).build()
            );

            Product p3 = Product.builder()
                    .brand(polycab)
                    .title("Polycab 4-Core 16 sq mm Aluminium Armoured Cable")
                    .slug("polycab-4-core-16-sqmm-armoured-cable")
                    .sku("ELE-CBL-4C-16AL")
                    .description("Heavy-duty underground armoured electrical cable for industrial power distribution.")
                    .imageUrl("https://cdn.hinchmart.com/products/armoured_cable.jpg")
                    .price(BigDecimal.valueOf(215.0))
                    .mrp(BigDecimal.valueOf(260.0))
                    .unit("Meters")
                    .moq(100)
                    .stockQty(5000)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .is24HourDelivery(true)
                    .rating(0.0)
                    .reviewCount(0)
                    .gstRate(BigDecimal.valueOf(18.0))
                    .hsnCode("8544")
                    .bulkPricingTiers(cableTiers)
                    .build();

            Product p4 = Product.builder()
                    .brand(astral)
                    .title("Astral CPVC Pro SDR 11 Pipe 1 Inch (3m)")
                    .slug("astral-cpvc-pro-sdr11-pipe-1inch")
                    .sku("PLM-CPVC-1IN-SDR11")
                    .description("Hot and cold water CPVC plumbing pipe conforming to ASTM D2846.")
                    .imageUrl("https://cdn.hinchmart.com/products/cpvc_pipe.jpg")
                    .price(BigDecimal.valueOf(540.0))
                    .mrp(BigDecimal.valueOf(620.0))
                    .unit("Pieces")
                    .moq(20)
                    .stockQty(800)
                    .active(true)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .is24HourDelivery(true)
                    .rating(0.0)
                    .reviewCount(0)
                    .gstRate(BigDecimal.valueOf(18.0))
                    .hsnCode("3917")
                    .build();

            Product savedP1 = productRepository.save(p1);
            Product savedP2 = productRepository.save(p2);
            Product savedP3 = productRepository.save(p3);
            Product savedP4 = productRepository.save(p4);

            initSampleReviewsAndOrders(savedP1, savedP2, savedP3, savedP4);
        }

        if (bannerRepository.count() == 0) {
            Banner b1 = Banner.builder()
                    .title("Bulk Savings 50% Off")
                    .subtitle("Direct manufacturer wholesale pricing on all TMT steel & cement.")
                    .imageUrl("https://cdn.hinchmart.com/banners/hero_steel_banner.jpg")
                    .linkType("CATEGORY")
                    .linkValue("civil-structural")
                    .position("HOME_HERO")
                    .sortOrder(1)
                    .active(true)
                    .build();

            Banner b2 = Banner.builder()
                    .title("24-Hour Express Jobsite Dispatch")
                    .subtitle("Guaranteed next-day delivery on site materials across Hyderabad & Telangana.")
                    .imageUrl("https://cdn.hinchmart.com/banners/express_delivery_banner.jpg")
                    .linkType("CATEGORY")
                    .linkValue("electrical-cables")
                    .position("HOME_HERO")
                    .sortOrder(2)
                    .active(true)
                    .build();

            bannerRepository.saveAll(List.of(b1, b2));
        }
    }

    private void initSampleReviewsAndOrders(Product p1, Product p2, Product p3, Product p4) {
        Customer c1 = customerRepository.findById(101).orElse(null);
        Customer c2 = customerRepository.findById(102).orElse(null);
        if (c1 == null) return;

        // Create a delivered order for verified purchase
        Order deliveredOrder = Order.builder()
                .orderNumber("ORD-20260815-101")
                .userId(c1.getCustomerId())
                .deliveryLocation("Plot 42, Financial District, Hyderabad")
                .subtotal(BigDecimal.valueOf(542000.0))
                .discount(BigDecimal.ZERO)
                .taxableAmount(BigDecimal.valueOf(542000.0))
                .cgst(BigDecimal.valueOf(48780.0))
                .sgst(BigDecimal.valueOf(48780.0))
                .igst(BigDecimal.ZERO)
                .totalGst(BigDecimal.valueOf(97560.0))
                .freightCharge(BigDecimal.valueOf(4500.0))
                .craneUnloadingCharge(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(644060.0))
                .paymentMethod("RAZORPAY")
                .paymentStatus("PAID")
                .orderStatus("DELIVERED")
                .carrierName("VRL Logistics Heavy Freight Fleet")
                .vehicleNumber("TS 09 UB 4412")
                .driverName("Ramesh Yadav")
                .trackingNumber("VRL-HYD-DEL-01")
                .estimatedDelivery(LocalDateTime.now().minusDays(5))
                .build();

        Order savedOrder = orderRepository.save(deliveredOrder);

        OrderItem item1 = OrderItem.builder()
                .order(savedOrder)
                .productId(p1.getProductId())
                .title(p1.getTitle())
                .imageUrl(p1.getImageUrl())
                .quantity(10)
                .unit("MT")
                .unitPrice(p1.getPrice())
                .originalPrice(p1.getPrice())
                .appliedTier("10 MT Tier")
                .gstRate(BigDecimal.valueOf(18.0))
                .lineTotal(BigDecimal.valueOf(542000.0))
                .lineGst(BigDecimal.valueOf(97560.0))
                .build();

        OrderItem item2 = OrderItem.builder()
                .order(savedOrder)
                .productId(p2.getProductId())
                .title(p2.getTitle())
                .imageUrl(p2.getImageUrl())
                .quantity(200)
                .unit("Bags")
                .unitPrice(p2.getPrice())
                .originalPrice(p2.getPrice())
                .appliedTier("200 Bags Tier")
                .gstRate(BigDecimal.valueOf(28.0))
                .lineTotal(BigDecimal.valueOf(76000.0))
                .lineGst(BigDecimal.valueOf(21280.0))
                .build();

        OrderItem savedItem1 = orderItemRepository.save(item1);
        OrderItem savedItem2 = orderItemRepository.save(item2);

        // Seed genuine product reviews with authentic ratings
        ProductReview review1 = ProductReview.builder()
                .product(p1)
                .customer(c1)
                .order(savedOrder)
                .orderItem(savedItem1)
                .rating(5)
                .title("Excellent Batch Quality and Prompt Delivery")
                .comment("Received test certificate conforming to Fe 550D standards. Bend test passed at our project site with zero issues.")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(4)
                .build();

        ProductReview review2 = ProductReview.builder()
                .product(p2)
                .customer(c1)
                .order(savedOrder)
                .orderItem(savedItem2)
                .rating(5)
                .title("Genuine Fresh Stock UltraTech PPC")
                .comment("Fresh manufacturing batch within 2 weeks of packing. Setting time and strength are spot on.")
                .status(ReviewStatus.APPROVED)
                .helpfulCount(2)
                .build();

        productReviewRepository.saveAll(List.of(review1, review2));

        // Dynamically compute and store product ratings and review counts
        recalculateAndSaveProductRating(p1.getProductId());
        recalculateAndSaveProductRating(p2.getProductId());
        recalculateAndSaveProductRating(p3.getProductId());
        recalculateAndSaveProductRating(p4.getProductId());
    }

    private void recalculateAndSaveProductRating(Integer productId) {
        productRepository.findById(productId).ifPresent(p -> {
            Double avg = productReviewRepository.averageRatingByProductAndStatus(productId, ReviewStatus.APPROVED);
            long count = productReviewRepository.countByProductAndStatus(productId, ReviewStatus.APPROVED);
            double rounded = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
            p.setRating(rounded);
            p.setReviewCount((int) count);
            productRepository.save(p);
        });
    }

    private void initSampleRfq() {
        if (rfqRepository.count() == 0) {
            Rfq rfq = Rfq.builder()
                    .rfqNumber("RFQ-2026-000601")
                    .userId(101)
                    .title("Bulk Procurement for G+14 Commercial Tower Project")
                    .category("Civil & Structural")
                    .productMaterial("TMT Rebars Fe 550D")
                    .quantity(100)
                    .unit("MT")
                    .technicalGrade("Fe 550D Primary (Tata/JSW/Jindal)")
                    .mtcRequired(true)
                    .deliveryLocation("Financial District, Gachibowli, Hyderabad - 500032")
                    .requiredByDate(LocalDate.now().plusDays(15))
                    .siteAccess("Heavy Trailer Access Available")
                    .craneRequired(true)
                    .targetBudget(BigDecimal.valueOf(5100000.0))
                    .paymentTerms("LETTER_OF_CREDIT")
                    .specifications("Only secondary bend test certified rebars accepted.")
                    .boqAttachmentUrl("https://cdn.hinchmart.com/rfq_docs/boq_project_tower_b.pdf")
                    .status("QUOTED")
                    .quotesCount(1)
                    .build();

            Rfq savedRfq = rfqRepository.save(rfq);

            Quotation quote = Quotation.builder()
                    .rfq(savedRfq)
                    .vendorId(45)
                    .vendorName("JSW Authorized Regional Yard")
                    .unitPrice(BigDecimal.valueOf(50800.0))
                    .totalAmount(BigDecimal.valueOf(5080000.0))
                    .deliveryLeadTimeDays(5)
                    .paymentTermsOffered("30 Days Credit / L/C accepted")
                    .mtcIncluded(true)
                    .freightIncluded(true)
                    .validUntil(LocalDateTime.now().plusDays(7))
                    .vendorRating(4.9)
                    .status("PENDING")
                    .build();

            quotationRepository.save(quote);

            RfqQuestion q = RfqQuestion.builder()
                    .rfq(savedRfq)
                    .question("Can the 100 MT consignment be delivered in 4 weekly batches?")
                    .response("Yes, weekly staged trailer dispatch is supported with advance scheduling.")
                    .status("ANSWERED")
                    .answeredAt(LocalDateTime.now())
                    .build();

            rfqQuestionRepository.save(q);
        }
    }

    private void initExtendedModules() {
        // 1. Reward Vouchers & Wallet
        if (rewardVoucherRepository.count() == 0) {
            rewardVoucherRepository.saveAll(List.of(
                    com.example.project.customer.entity.RewardVoucher.builder()
                            .code("BUILDER50K")
                            .title("Commercial Project Flat Discount")
                            .description("Flat INR 50,000 off on bulk orders over INR 5 Lakhs")
                            .discountType("FIXED")
                            .discountValue(new BigDecimal("50000.00"))
                            .minOrderValue(new BigDecimal("500000.00"))
                            .maxDiscount(new BigDecimal("50000.00"))
                            .expiryDate(LocalDateTime.now().plusMonths(6))
                            .redeemed(false)
                            .active(true)
                            .build(),
                    com.example.project.customer.entity.RewardVoucher.builder()
                            .code("HINCH10")
                            .title("10% Construction Site Kickoff")
                            .description("10% instant discount up to INR 25,000 for verified builders")
                            .discountType("PERCENTAGE")
                            .discountValue(new BigDecimal("10.00"))
                            .minOrderValue(new BigDecimal("100000.00"))
                            .maxDiscount(new BigDecimal("25000.00"))
                            .expiryDate(LocalDateTime.now().plusMonths(3))
                            .redeemed(false)
                            .active(true)
                            .build(),
                    com.example.project.customer.entity.RewardVoucher.builder()
                            .code("STEEL5")
                            .title("Primary Steel Concession")
                            .description("5% concession on Tata & JSW Fe550D TMT consignments")
                            .discountType("PERCENTAGE")
                            .discountValue(new BigDecimal("5.00"))
                            .minOrderValue(new BigDecimal("200000.00"))
                            .maxDiscount(new BigDecimal("15000.00"))
                            .expiryDate(LocalDateTime.now().plusMonths(4))
                            .redeemed(false)
                            .active(true)
                            .build()
            ));
        }

        if (walletRepository.findByUserId(101).isEmpty()) {
            com.example.project.customer.entity.Wallet wallet = com.example.project.customer.entity.Wallet.builder()
                    .userId(101)
                    .balance(new BigDecimal("75000.00"))
                    .currency("INR")
                    .loyaltyPoints(1850)
                    .tier("PLATINUM")
                    .active(true)
                    .build();
            com.example.project.customer.entity.Wallet savedWallet = walletRepository.save(wallet);

            walletTransactionRepository.save(com.example.project.customer.entity.WalletTransaction.builder()
                    .wallet(savedWallet)
                    .type("CREDIT")
                    .amount(new BigDecimal("75000.00"))
                    .source("ENTERPRISE_CREDIT_APPROVAL")
                    .referenceId("CR-20260801-101")
                    .description("Approved B2B revolving credit line deposit")
                    .balanceAfter(new BigDecimal("75000.00"))
                    .timestamp(LocalDateTime.now().minusDays(15))
                    .build());
        }

        // 2. Equipment Rentals
        if (rentalEquipmentRepository.count() == 0) {
            rentalEquipmentRepository.saveAll(List.of(
                    com.example.project.customer.entity.RentalEquipment.builder()
                            .name("JCB 3DX Plus Backhoe Loader & Excavator")
                            .category("EXCAVATOR")
                            .model("2025 EcoXcellence 76 HP")
                            .specifications("Max dig depth: 4.77m, Loader bucket capacity: 1.1 cu.m, High-traction 4WD, Air-conditioned operator cabin.")
                            .dailyRate(new BigDecimal("8500.00"))
                            .weeklyRate(new BigDecimal("52000.00"))
                            .monthlyRate(new BigDecimal("185000.00"))
                            .depositAmount(new BigDecimal("25000.00"))
                            .imageUrl("https://cdn.hinchmart.com/rentals/jcb_3dx.jpg")
                            .location("Hyderabad Hub - Nanakramguda")
                            .operatorAvailable(true)
                            .operatorDailyCharge(new BigDecimal("1200.00"))
                            .available(true)
                            .build(),
                    com.example.project.customer.entity.RentalEquipment.builder()
                            .name("SANY STC250C 25-Ton Hydraulic Truck Crane")
                            .category("CRANE")
                            .model("STC250C 4-Section U-Shape Boom")
                            .specifications("Max lifting capacity: 25T, Max boom reach: 33.5m, Full 360-degree slew, Wireless safe load indicator (SLI).")
                            .dailyRate(new BigDecimal("22000.00"))
                            .weeklyRate(new BigDecimal("135000.00"))
                            .monthlyRate(new BigDecimal("480000.00"))
                            .depositAmount(new BigDecimal("50000.00"))
                            .imageUrl("https://cdn.hinchmart.com/rentals/sany_crane.jpg")
                            .location("Hyderabad Hub - Patancheru")
                            .operatorAvailable(true)
                            .operatorDailyCharge(new BigDecimal("2000.00"))
                            .available(true)
                            .build(),
                    com.example.project.customer.entity.RentalEquipment.builder()
                            .name("Heavy-Duty Cuplock Scaffolding System (5,000 sq.ft)")
                            .category("SCAFFOLDING")
                            .model("EN 12810 Heavy Industrial")
                            .specifications("Galvanized high-yield steel standards, ledgers, base jacks, and steel walking planks. Rated for 450 kg/m2 load.")
                            .dailyRate(new BigDecimal("2500.00"))
                            .weeklyRate(new BigDecimal("15000.00"))
                            .monthlyRate(new BigDecimal("50000.00"))
                            .depositAmount(new BigDecimal("20000.00"))
                            .imageUrl("https://cdn.hinchmart.com/rentals/cuplock_scaffold.jpg")
                            .location("Hyderabad Hub - HITEC City")
                            .operatorAvailable(false)
                            .operatorDailyCharge(BigDecimal.ZERO)
                            .available(true)
                            .build(),
                    com.example.project.customer.entity.RentalEquipment.builder()
                            .name("Cummins 125 kVA Soundproof Silent Diesel Generator")
                            .category("GENERATOR")
                            .model("CP125D5 CPCB-IV+ Compliant")
                            .specifications("Prime power rating 125 kVA / 100 kWe, 415V 50Hz 3-phase, Acoustic enclosure < 75 dBA at 1m, 240L fuel tank.")
                            .dailyRate(new BigDecimal("4500.00"))
                            .weeklyRate(new BigDecimal("28000.00"))
                            .monthlyRate(new BigDecimal("95000.00"))
                            .depositAmount(new BigDecimal("15000.00"))
                            .imageUrl("https://cdn.hinchmart.com/rentals/cummins_dg.jpg")
                            .location("Hyderabad Hub - Shamshabad")
                            .operatorAvailable(true)
                            .operatorDailyCharge(new BigDecimal("1000.00"))
                            .available(true)
                            .build()
            ));
        }

        // 3. Purchase Orders
        if (purchaseOrderRepository.count() == 0) {
            com.example.project.customer.entity.PurchaseOrder po1 = com.example.project.customer.entity.PurchaseOrder.builder()
                    .poNumber("PO-20260818-001")
                    .userId(101)
                    .vendorId(1001)
                    .totalAmount(new BigDecimal("330400.00"))
                    .status("APPROVED")
                    .deliveryDate(LocalDate.now().plusDays(5))
                    .billingAddress("Skyline Infra Ventures Ltd, HITEC City, Hyderabad, TS - 500081")
                    .shippingAddress("Project Site #4B, Financial District, Hyderabad, TS")
                    .paymentTerms("NET_30")
                    .notes("Dispatch only primary producer rebars with standard manufacturer test certificates.")
                    .approvedAt(LocalDateTime.now().minusDays(2))
                    .approvedBy("Senior Procurement Lead")
                    .build();

            com.example.project.customer.entity.PurchaseOrder savedPo1 = purchaseOrderRepository.save(po1);

            purchaseOrderItemRepository.save(com.example.project.customer.entity.PurchaseOrderItem.builder()
                    .purchaseOrder(savedPo1)
                    .productId(1)
                    .productTitle("Tata Tiscon 550D TMT Rebars 12mm (Bundle)")
                    .quantity(5)
                    .unit("Metric Ton")
                    .unitPrice(new BigDecimal("56000.00"))
                    .taxRate(new BigDecimal("18.00"))
                    .lineTotal(new BigDecimal("330400.00"))
                    .build());
        }

        // 4. Chat Conversation
        if (conversationRepository.count() == 0) {
            com.example.project.customer.entity.Conversation conv = com.example.project.customer.entity.Conversation.builder()
                    .buyerId(101)
                    .sellerId(1001)
                    .topic("PRODUCT")
                    .referenceId("PROD-1")
                    .title("Fe550D Rebar Consignment Logistics & MTC")
                    .lastMessageText("Yes, mill test certificates are attached with every dispatched trailer.")
                    .lastMessageTimestamp(LocalDateTime.now().minusHours(2))
                    .unreadBuyer(0)
                    .unreadSeller(0)
                    .build();
            com.example.project.customer.entity.Conversation savedConv = conversationRepository.save(conv);

            chatMessageRepository.saveAll(List.of(
                    com.example.project.customer.entity.ChatMessage.builder()
                            .conversation(savedConv)
                            .senderId(101)
                            .senderRole("BUYER")
                            .content("Hello, we are planning a 20 MT order for Tata Tiscon Fe550D 16mm. Can you confirm the dispatch timeline to Gachibowli?")
                            .messageType("TEXT")
                            .isRead(true)
                            .timestamp(LocalDateTime.now().minusHours(4))
                            .build(),
                    com.example.project.customer.entity.ChatMessage.builder()
                            .conversation(savedConv)
                            .senderId(1001)
                            .senderRole("SELLER")
                            .content("Hi! We have direct stock available at our Patancheru central yard. We can dispatch within 24 hours of PO confirmation.")
                            .messageType("TEXT")
                            .isRead(true)
                            .timestamp(LocalDateTime.now().minusHours(3))
                            .build(),
                    com.example.project.customer.entity.ChatMessage.builder()
                            .conversation(savedConv)
                            .senderId(1001)
                            .senderRole("SELLER")
                            .content("Yes, mill test certificates are attached with every dispatched trailer.")
                            .messageType("TEXT")
                            .isRead(true)
                            .timestamp(LocalDateTime.now().minusHours(2))
                            .build()
            ));
        }

        // 5. Blog Articles
        if (blogArticleRepository.count() == 0) {
            blogArticleRepository.saveAll(List.of(
                    com.example.project.customer.entity.BlogArticle.builder()
                            .title("How to Verify Mill Test Certificates (MTC) for Fe550D TMT Rebars")
                            .slug("verify-mtc-fe550d-rebars")
                            .excerpt("A technical guide for project site engineers on validating chemical compositions, yield strength, and BIS 1786 compliance.")
                            .content("Ensuring structural integrity starts with material verification. Mill Test Certificates (MTC) provide proof of yield stress (min 550 N/mm2), elongation (min 14.5%), and carbon equivalent (CE max 0.42%). Always check heat numbers etched on rebar bundles against the certificate before unloading on site.")
                            .author("Dr. V. Ramanathan, Chief Structural Consultant")
                            .category("MATERIAL_TESTING")
                            .tags("tmt,steel,quality,bis-1786")
                            .readTimeMinutes(6)
                            .imageUrl("https://cdn.hinchmart.com/blog/mtc_verification.jpg")
                            .published(true)
                            .publishedAt(LocalDateTime.now().minusDays(10))
                            .build(),
                    com.example.project.customer.entity.BlogArticle.builder()
                            .title("Navigating Split GST & Input Tax Credit (ITC) on Bulk Construction Procurement")
                            .slug("split-gst-input-tax-credit-infra")
                            .excerpt("Optimizing working capital and tax compliance for infrastructure contractors and enterprise developers in India.")
                            .content("Procuring bulk cement, steel, and aggregate involves multiple GST slabs (18% for steel, 28% for cement). Understanding the place of supply and ensuring supplier filing on GSTR-1 directly impacts your monthly Input Tax Credit flow. Learn how HinchMart automates split tax reporting.")
                            .author("Ananya Sengupta, Head of B2B Tax & Finance")
                            .category("PROCUREMENT")
                            .tags("gst,tax,b2b,invoicing")
                            .readTimeMinutes(8)
                            .imageUrl("https://cdn.hinchmart.com/blog/b2b_tax.jpg")
                            .published(true)
                            .publishedAt(LocalDateTime.now().minusDays(4))
                            .build()
            ));
        }

        // 6. News Items
        if (newsItemRepository.count() == 0) {
            newsItemRepository.saveAll(List.of(
                    com.example.project.customer.entity.NewsItem.builder()
                            .title("TMT Steel Ex-Yard Prices Consolidate with 2.4% Upward Trend Across Southern Hubs")
                            .summary("Primary mills report strong commercial and metro infrastructure demand holding prices firm at INR 52,000 - 54,000 per MT.")
                            .content("Primary rebar manufacturers Tata Steel, JSW, and SAIL experienced strong volume absorption from state highway developments and urban infrastructure packages, leading to a mild 2.4% price consolidation.")
                            .category("COMMODITY_PRICES")
                            .source("HinchMart Market Intelligence")
                            .priceChangePercentage(2.4)
                            .trendDirection("UP")
                            .publishedAt(LocalDateTime.now().minusHours(8))
                            .build(),
                    com.example.project.customer.entity.NewsItem.builder()
                            .title("UltraTech & ACC Cement Maintain Stable Pricing for Ready-Mix Concrete Grades")
                            .summary("Bulk cement supply remains fluid across Telangana and Karnataka with prompt 48-hour delivery.")
                            .content("OPC 53 and PPC bulk grades trade steady at INR 340 - 360 per bag. Demand from precast fabrication yards is expected to peak over the upcoming quarter.")
                            .category("COMMODITY_PRICES")
                            .source("National Infrastructure News")
                            .priceChangePercentage(0.0)
                            .trendDirection("STABLE")
                            .publishedAt(LocalDateTime.now().minusHours(24))
                            .build()
            ));
        }

        // 7. Support Tickets
        if (supportTicketRepository.count() == 0) {
            com.example.project.customer.entity.SupportTicket tkt = com.example.project.customer.entity.SupportTicket.builder()
                    .ticketNumber("TKT-20260825-001")
                    .userId(101)
                    .subject("Crane Unloading Request & Slot Confirmation for Site Delivery")
                    .category("DELIVERY")
                    .priority("HIGH")
                    .status("RESOLVED")
                    .orderId(1)
                    .build();
            com.example.project.customer.entity.SupportTicket savedTkt = supportTicketRepository.save(tkt);

            ticketMessageRepository.saveAll(List.of(
                    com.example.project.customer.entity.TicketMessage.builder()
                            .ticket(savedTkt)
                            .senderId(101)
                            .senderRole("USER")
                            .senderName("Customer")
                            .content("Hello, our site gate requires an articulated 25-Ton hydraulic crane for unloading the 16mm rebar trailer. Please confirm the operator arrival time.")
                            .timestamp(LocalDateTime.now().minusDays(3))
                            .build(),
                    com.example.project.customer.entity.TicketMessage.builder()
                            .ticket(savedTkt)
                            .senderId(1)
                            .senderRole("SUPPORT_AGENT")
                            .senderName("HinchMart Logistics Team")
                            .content("Hello, we have assigned crane unit CR-12 with certified rigger. Arrival scheduled at 08:30 AM tomorrow. Driver contact details shared on tracking link.")
                            .timestamp(LocalDateTime.now().minusDays(2))
                            .build()
            ));
        }
    }
}
