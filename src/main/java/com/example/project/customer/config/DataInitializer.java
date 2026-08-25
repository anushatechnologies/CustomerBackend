package com.example.project.customer.config;

import com.example.project.customer.entity.*;
import com.example.project.customer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final BannerRepository bannerRepository;
    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;
    private final RfqRepository rfqRepository;
    private final RfqQuotationRepository quotationRepository;
    private final RfqQuestionRepository questionRepository;
    private final OrderRepository orderRepository;

    public DataInitializer(CategoryRepository categoryRepository,
                           SubcategoryRepository subcategoryRepository,
                           ProductRepository productRepository,
                           VendorRepository vendorRepository,
                           BannerRepository bannerRepository,
                           UserProfileRepository userProfileRepository,
                           AddressRepository addressRepository,
                           RfqRepository rfqRepository,
                           RfqQuotationRepository quotationRepository,
                           RfqQuestionRepository questionRepository,
                           OrderRepository orderRepository) {
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.bannerRepository = bannerRepository;
        this.userProfileRepository = userProfileRepository;
        this.addressRepository = addressRepository;
        this.rfqRepository = rfqRepository;
        this.quotationRepository = quotationRepository;
        this.questionRepository = questionRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        try {
            seedUserProfile();
            seedAddresses();
            seedVendorsAndCatalog();
            seedBanners();
            seedRfqs();
            log.info("HINCH MART database successfully initialized with B2B sample data.");
        } catch (Exception e) {
            log.warn("Data initialization skipped or already present: {}", e.getMessage());
        }
    }

    private void seedUserProfile() {
        if (userProfileRepository.count() == 0) {
            UserProfile profile = new UserProfile(
                    101,
                    "Rajesh Sharma",
                    "9876543210",
                    "rajesh@apexbldrs.com",
                    "BUYER",
                    "GOLD",
                    "Apex Infra Projects Pvt Ltd",
                    "36AAACT2727Q1ZW",
                    "AAACT2727Q",
                    "General Contractor",
                    true,
                    new BigDecimal("5000000.00"),
                    new BigDecimal("3250000.00")
            );
            userProfileRepository.save(profile);
        }
    }

    private void seedAddresses() {
        if (addressRepository.count() == 0) {
            Address a1 = new Address();
            a1.setUserId(101);
            a1.setSiteName("Tower B Project Site");
            a1.setRecipientName("Site Eng. Vikram Reddy");
            a1.setPhone("9849112233");
            a1.setAddressLine1("Plot 42, Financial District");
            a1.setCity("Hyderabad");
            a1.setState("Telangana");
            a1.setPincode("500032");
            a1.setLandmark("Near Wave Rock");
            a1.setDefault(true);
            a1.setHasHeavyVehicleAccess(true);
            addressRepository.save(a1);

            Address a2 = new Address();
            a2.setUserId(101);
            a2.setSiteName("Hitech City Phase 2 Site");
            a2.setRecipientName("Site Eng. Vikram Reddy");
            a2.setPhone("9849112233");
            a2.setAddressLine1("Sy No 88, Mindspace Circle");
            a2.setCity("Hyderabad");
            a2.setState("Telangana");
            a2.setPincode("500081");
            a2.setLandmark("Opposite Inorbit Mall");
            a2.setDefault(false);
            a2.setHasHeavyVehicleAccess(true);
            addressRepository.save(a2);
        }
    }

    private void seedVendorsAndCatalog() {
        if (categoryRepository.count() == 0) {
            // Vendors
            Vendor v1 = vendorRepository.save(new Vendor("Tata Steel Distribution Yard", "Hyderabad", true, 4.9));
            Vendor v2 = vendorRepository.save(new Vendor("JSW Authorized Regional Yard", "Hyderabad", true, 4.9));
            Vendor v3 = vendorRepository.save(new Vendor("UltraTech Building Solutions Depot", "Hyderabad", true, 4.8));
            Vendor v4 = vendorRepository.save(new Vendor("Havells Industrial Power Hub", "Hyderabad", true, 4.8));

            // Categories
            Category c1 = new Category();
            c1.setName("Civil & Structural");
            c1.setSlug("civil-structural");
            c1.setImageUrl("https://images.unsplash.com/photo-1541888946425-d0fbb18f15f6?w=800&auto=format&fit=crop&q=80");
            c1.setActive(true);
            c1.setSortOrder(1);
            Category savedC1 = categoryRepository.save(c1);

            Category c2 = new Category();
            c2.setName("Electrical & Cables");
            c2.setSlug("electrical-cables");
            c2.setImageUrl("https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&auto=format&fit=crop&q=80");
            c2.setActive(true);
            c2.setSortOrder(2);
            Category savedC2 = categoryRepository.save(c2);

            Category c3 = new Category();
            c3.setName("Plumbing & Sanitary");
            c3.setSlug("plumbing-sanitary");
            c3.setImageUrl("https://images.unsplash.com/photo-1585704032915-c3400ca199e7?w=800&auto=format&fit=crop&q=80");
            c3.setActive(true);
            c3.setSortOrder(3);
            Category savedC3 = categoryRepository.save(c3);

            Category c4 = new Category();
            c4.setName("Paints & Finishes");
            c4.setSlug("paints-finishes");
            c4.setImageUrl("https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=800&auto=format&fit=crop&q=80");
            c4.setActive(true);
            c4.setSortOrder(4);
            Category savedC4 = categoryRepository.save(c4);

            // Subcategories
            Subcategory s1 = new Subcategory();
            s1.setCategory(savedC1);
            s1.setName("TMT Steel & Rebars");
            s1.setSlug("tmt-steel-rebars");
            s1.setImageUrl("https://images.unsplash.com/photo-1504917599217-d4dc5ebe6122?w=800&auto=format&fit=crop&q=80");
            s1.setActive(true);
            s1.setSortOrder(1);
            Subcategory savedS1 = subcategoryRepository.save(s1);

            Subcategory s2 = new Subcategory();
            s2.setCategory(savedC1);
            s2.setName("Cement & RMC");
            s2.setSlug("cement-rmc");
            s2.setImageUrl("https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80");
            s2.setActive(true);
            s2.setSortOrder(2);
            Subcategory savedS2 = subcategoryRepository.save(s2);

            Subcategory s3 = new Subcategory();
            s3.setCategory(savedC2);
            s3.setName("Armoured Power Cables");
            s3.setSlug("armoured-power-cables");
            s3.setImageUrl("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&auto=format&fit=crop&q=80");
            s3.setActive(true);
            s3.setSortOrder(1);
            Subcategory savedS3 = subcategoryRepository.save(s3);

            Subcategory s4 = new Subcategory();
            s4.setCategory(savedC4);
            s4.setName("Industrial Epoxy & Coatings");
            s4.setSlug("industrial-epoxy-coatings");
            s4.setImageUrl("https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80");
            s4.setActive(true);
            s4.setSortOrder(1);
            Subcategory savedS4 = subcategoryRepository.save(s4);

            // Product 1: TMT Steel
            Product p1 = new Product();
            p1.setCategory(savedC1);
            p1.setSubcategory(savedS1);
            p1.setTitle("TMT Steel Rebars Fe 550D (12mm)");
            p1.setSlug("tmt-steel-rebars-fe-550d-12mm");
            p1.setSku("STL-TMT-12-FE550D");
            p1.setBrand("Tata Tiscon");
            p1.setDescription("High-ductility primary steel rebars conforming to IS 1786 standards.");
            p1.setImageUrl("https://images.unsplash.com/photo-1504917599217-d4dc5ebe6122?w=800&auto=format&fit=crop&q=80");
            p1.setImages(Arrays.asList(
                    "https://images.unsplash.com/photo-1504917599217-d4dc5ebe6122?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1587293852726-70cdb56c2866?w=800&auto=format&fit=crop&q=80"
            ));
            p1.setPrice(new BigDecimal("54200.00"));
            p1.setMrp(new BigDecimal("59000.00"));
            p1.setUnit("MT");
            p1.setMoq(5);
            p1.setStockQty(500);
            p1.setActive(true);
            p1.setIs24HourDelivery(true);
            p1.setRating(4.8);
            p1.setReviewCount(42);
            p1.setGstRate(18.0);
            p1.setHsnCode("7214");
            p1.setVendor(v1);

            Map<String, String> specs1 = new HashMap<>();
            specs1.put("Standard", "IS 1786:2008");
            specs1.put("Grade", "Fe 550D");
            specs1.put("Diameter", "12 mm");
            specs1.put("Yield Strength", "550 N/mm²");
            specs1.put("Manufacturer Test Certificate (MTC)", "Included per batch");
            p1.setSpecifications(specs1);

            p1.addBulkPricingTier(new BulkPricingTier(5, 19, new BigDecimal("54200.00"), 8.1));
            p1.addBulkPricingTier(new BulkPricingTier(20, 49, new BigDecimal("52800.00"), 10.5));
            p1.addBulkPricingTier(new BulkPricingTier(50, null, new BigDecimal("51200.00"), 13.2));
            productRepository.save(p1);

            // Product 2: Cement
            Product p2 = new Product();
            p2.setCategory(savedC1);
            p2.setSubcategory(savedS2);
            p2.setTitle("UltraTech Premium PPC Cement (50kg Bag)");
            p2.setSlug("ultratech-premium-ppc-cement-50kg");
            p2.setSku("CEM-UT-PPC-50KG");
            p2.setBrand("UltraTech");
            p2.setDescription("High-performance Portland Pozzolana Cement for structural RCC works.");
            p2.setImageUrl("https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80");
            p2.setImages(List.of("https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80"));
            p2.setPrice(new BigDecimal("380.00"));
            p2.setMrp(new BigDecimal("420.00"));
            p2.setUnit("Bags");
            p2.setMoq(50);
            p2.setStockQty(2500);
            p2.setActive(true);
            p2.setIs24HourDelivery(true);
            p2.setRating(4.9);
            p2.setReviewCount(88);
            p2.setGstRate(28.0);
            p2.setHsnCode("2523");
            p2.setVendor(v3);

            p2.addBulkPricingTier(new BulkPricingTier(50, 199, new BigDecimal("380.00"), 9.5));
            p2.addBulkPricingTier(new BulkPricingTier(200, 499, new BigDecimal("365.00"), 13.1));
            p2.addBulkPricingTier(new BulkPricingTier(500, null, new BigDecimal("350.00"), 16.7));
            productRepository.save(p2);

            // Product 3: Cable
            Product p3 = new Product();
            p3.setCategory(savedC2);
            p3.setSubcategory(savedS3);
            p3.setTitle("Havells 4-Core 16 sq mm Aluminium Armoured Cable");
            p3.setSlug("havells-4-core-16sqmm-armoured-cable");
            p3.setSku("ELE-HAV-4C16-ARM");
            p3.setBrand("Havells");
            p3.setDescription("XLPE insulated heavy duty underground power cable conforming to IS 7098.");
            p3.setImageUrl("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&auto=format&fit=crop&q=80");
            p3.setImages(List.of("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&auto=format&fit=crop&q=80"));
            p3.setPrice(new BigDecimal("215.00"));
            p3.setMrp(new BigDecimal("260.00"));
            p3.setUnit("Meter");
            p3.setMoq(100);
            p3.setStockQty(1200);
            p3.setActive(true);
            p3.setIs24HourDelivery(false);
            p3.setRating(4.7);
            p3.setReviewCount(29);
            p3.setGstRate(18.0);
            p3.setHsnCode("8544");
            p3.setVendor(v4);

            p3.addBulkPricingTier(new BulkPricingTier(100, 499, new BigDecimal("215.00"), 17.3));
            p3.addBulkPricingTier(new BulkPricingTier(500, null, new BigDecimal("198.00"), 23.8));
            productRepository.save(p3);

            // Product 4: Epoxy
            Product p4 = new Product();
            p4.setCategory(savedC4);
            p4.setSubcategory(savedS4);
            p4.setTitle("Asian Paints Apex Ultima Protective Epoxy Coating (20L)");
            p4.setSlug("asian-paints-apex-ultima-epoxy-20l");
            p4.setSku("PNT-AP-ULT-20L");
            p4.setBrand("Asian Paints");
            p4.setDescription("Heavy-duty anti-corrosive water-resistant industrial coating.");
            p4.setImageUrl("https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=800&auto=format&fit=crop&q=80");
            p4.setImages(List.of("https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=800&auto=format&fit=crop&q=80"));
            p4.setPrice(new BigDecimal("6800.00"));
            p4.setMrp(new BigDecimal("7600.00"));
            p4.setUnit("Bucket");
            p4.setMoq(2);
            p4.setStockQty(150);
            p4.setActive(true);
            p4.setIs24HourDelivery(true);
            p4.setRating(4.6);
            p4.setReviewCount(17);
            p4.setGstRate(18.0);
            p4.setHsnCode("3208");
            productRepository.save(p4);
        }
    }

    private void seedBanners() {
        if (bannerRepository.count() == 0) {
            Banner b1 = new Banner();
            b1.setTitle("Bulk Savings 50% Off");
            b1.setSubtitle("Direct manufacturer wholesale pricing on all TMT steel & cement.");
            b1.setImageUrl("https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=1200&auto=format&fit=crop&q=80");
            b1.setLinkType("CATEGORY");
            b1.setLinkValue("civil-structural");
            b1.setPosition("HOME_HERO");
            b1.setSortOrder(1);
            b1.setActive(true);
            bannerRepository.save(b1);

            Banner b2 = new Banner();
            b2.setTitle("24h Express Jobsite Delivery");
            b2.setSubtitle("Guaranteed next-morning dispatch for urgent commercial orders.");
            b2.setImageUrl("https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=1200&auto=format&fit=crop&q=80");
            b2.setLinkType("PRODUCT");
            b2.setLinkValue("tmt-steel-rebars-fe-550d-12mm");
            b2.setPosition("HOME_HERO");
            b2.setSortOrder(2);
            b2.setActive(true);
            bannerRepository.save(b2);
        }
    }

    private void seedRfqs() {
        if (rfqRepository.count() == 0) {
            Rfq rfq = new Rfq();
            rfq.setUserId(101);
            rfq.setRfqNumber("RFQ-2026-000601");
            rfq.setTitle("Bulk Procurement for G+14 Commercial Tower Project");
            rfq.setCategory("Steel & TMT");
            rfq.setProductMaterial("TMT Rebars Fe 550D");
            rfq.setQuantity(100);
            rfq.setUnit("MT");
            rfq.setTechnicalGrade("Fe 550D Primary (Tata/JSW/Jindal)");
            rfq.setMtcRequired(true);
            rfq.setDeliveryLocation("Financial District, Gachibowli, Hyderabad - 500032");
            rfq.setRequiredByDate(LocalDate.now().plusDays(15));
            rfq.setSiteAccess("Heavy Trailer Access Available");
            rfq.setCraneRequired(true);
            rfq.setTargetBudget(new BigDecimal("5100000.00"));
            rfq.setPaymentTerms("LETTER_OF_CREDIT");
            rfq.setSpecifications("Only secondary bend test certified rebars accepted.");
            rfq.setBoqAttachmentUrl("https://cdn.hinchmart.com/rfq_docs/boq_project_tower_b.pdf");
            rfq.setStatus("OPEN");

            Rfq savedRfq = rfqRepository.save(rfq);

            RfqQuotation q1 = new RfqQuotation(
                    45,
                    "JSW Authorized Regional Yard",
                    new BigDecimal("50800.00"),
                    new BigDecimal("5080000.00"),
                    5,
                    "30 Days Credit / L/C accepted",
                    true,
                    true,
                    LocalDateTime.now().plusDays(7),
                    4.9,
                    "PENDING"
            );
            savedRfq.addQuotation(q1);

            RfqQuestion qst1 = new RfqQuestion(
                    "Can the 100 MT consignment be delivered in 4 weekly batches?",
                    "Yes, weekly dispatch schedules with weighbridge certificates can be arranged.",
                    "ANSWERED"
            );
            savedRfq.addQuestion(qst1);

            rfqRepository.save(savedRfq);
        }
    }
}
