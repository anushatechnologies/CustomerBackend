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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
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
}
