package com.example.project.customer.config;

import com.example.project.customer.entity.Address;
import com.example.project.customer.entity.Banner;
import com.example.project.customer.entity.BulkPricingTier;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Quotation;
import com.example.project.customer.entity.Rfq;
import com.example.project.customer.entity.RfqQuestion;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.UserProfile;
import com.example.project.customer.entity.VendorInfo;
import com.example.project.customer.repository.AddressRepository;
import com.example.project.customer.repository.BannerRepository;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final BannerRepository bannerRepository;
    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;
    private final RfqRepository rfqRepository;
    private final QuotationRepository quotationRepository;
    private final RfqQuestionRepository rfqQuestionRepository;

    @Override
    public void run(String... args) {
        try {
            initUserProfile();
            initAddresses();
            initCatalogAndBanners();
            initSampleRfq();
            log.info("HINCH MART database successfully initialized with rich B2B catalog and seed data.");
        } catch (Exception e) {
            log.warn("Data initialization skipped or already present: {}", e.getMessage());
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
                    .landmark("Near Wave Rock")
                    .isDefault(true)
                    .hasHeavyVehicleAccess(true)
                    .build();

            Address site2 = Address.builder()
                    .siteName("Highway Expressway Yard #4")
                    .recipientName("Supervisor Anand Rao")
                    .phone("9849556677")
                    .addressLine1("Survey 112, ORR Service Road")
                    .city("Hyderabad")
                    .state("Telangana")
                    .pincode("500075")
                    .landmark("Exit 11 Toll Plaza")
                    .isDefault(false)
                    .hasHeavyVehicleAccess(true)
                    .build();

            addressRepository.saveAll(List.of(site1, site2));
        }
    }

    private void initCatalogAndBanners() {
        if (categoryRepository.count() == 0) {
            Category civil = categoryRepository.save(Category.builder()
                    .name("Civil & Structural")
                    .slug("civil-structural")
                    .imageUrl("https://cdn.hinchmart.com/categories/civil_structural.jpg")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Category electrical = categoryRepository.save(Category.builder()
                    .name("Electrical & Cables")
                    .slug("electrical-cables")
                    .imageUrl("https://cdn.hinchmart.com/categories/electrical.jpg")
                    .active(true)
                    .sortOrder(2)
                    .build());

            Category plumbing = categoryRepository.save(Category.builder()
                    .name("Plumbing & Sanitary")
                    .slug("plumbing-sanitary")
                    .imageUrl("https://cdn.hinchmart.com/categories/plumbing.jpg")
                    .active(true)
                    .sortOrder(3)
                    .build());

            Category paints = categoryRepository.save(Category.builder()
                    .name("Paints & Finishes")
                    .slug("paints-finishes")
                    .imageUrl("https://cdn.hinchmart.com/categories/paints.jpg")
                    .active(true)
                    .sortOrder(4)
                    .build());

            // Subcategories
            Subcategory tmt = subcategoryRepository.save(Subcategory.builder()
                    .category(civil)
                    .name("TMT Steel & Rebars")
                    .slug("tmt-steel-rebars")
                    .imageUrl("https://cdn.hinchmart.com/subcategories/tmt_steel.jpg")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Subcategory cement = subcategoryRepository.save(Subcategory.builder()
                    .category(civil)
                    .name("Cement & RMC")
                    .slug("cement-rmc")
                    .imageUrl("https://cdn.hinchmart.com/subcategories/cement.jpg")
                    .active(true)
                    .sortOrder(2)
                    .build());

            Subcategory cables = subcategoryRepository.save(Subcategory.builder()
                    .category(electrical)
                    .name("Armoured XLPE Cables")
                    .slug("armoured-cables")
                    .imageUrl("https://cdn.hinchmart.com/subcategories/armoured_cables.jpg")
                    .active(true)
                    .sortOrder(1)
                    .build());

            Subcategory pipes = subcategoryRepository.save(Subcategory.builder()
                    .category(plumbing)
                    .name("CPVC & UPVC Pipes")
                    .slug("cpvc-pipes")
                    .imageUrl("https://cdn.hinchmart.com/subcategories/pipes.jpg")
                    .active(true)
                    .sortOrder(1)
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
                    .subcategory(tmt)
                    .title("TMT Steel Rebars Fe 550D (12mm)")
                    .slug("tmt-steel-rebars-fe-550d-12mm")
                    .sku("STL-TMT-12-FE550D")
                    .brand("Tata Tiscon")
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
                    .is24HourDelivery(true)
                    .rating(4.8)
                    .reviewCount(42)
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
                    .subcategory(cement)
                    .title("UltraTech Super Cement PPC (50kg Bag)")
                    .slug("ultratech-super-cement-ppc-50kg")
                    .sku("CMT-PPC-50KG-UT")
                    .brand("UltraTech")
                    .description("Premium Portland Pozzolana Cement for high durability concrete construction.")
                    .imageUrl("https://cdn.hinchmart.com/products/ultratech_cement.jpg")
                    .images(List.of("https://cdn.hinchmart.com/products/ultratech_cement_1.jpg"))
                    .price(BigDecimal.valueOf(380.0))
                    .mrp(BigDecimal.valueOf(420.0))
                    .unit("Bags")
                    .moq(100)
                    .stockQty(2500)
                    .active(true)
                    .is24HourDelivery(true)
                    .rating(4.9)
                    .reviewCount(128)
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
                    .subcategory(cables)
                    .title("Polycab 4-Core 16 sq mm Aluminium Armoured Cable")
                    .slug("polycab-4-core-16-sqmm-armoured-cable")
                    .sku("ELE-CBL-4C-16AL")
                    .brand("Polycab")
                    .description("Heavy-duty underground armoured electrical cable for industrial power distribution.")
                    .imageUrl("https://cdn.hinchmart.com/products/armoured_cable.jpg")
                    .price(BigDecimal.valueOf(215.0))
                    .mrp(BigDecimal.valueOf(260.0))
                    .unit("Meters")
                    .moq(100)
                    .stockQty(5000)
                    .active(true)
                    .is24HourDelivery(true)
                    .rating(4.7)
                    .reviewCount(31)
                    .gstRate(BigDecimal.valueOf(18.0))
                    .hsnCode("8544")
                    .bulkPricingTiers(cableTiers)
                    .build();

            Product p4 = Product.builder()
                    .subcategory(pipes)
                    .title("Astral CPVC Pro SDR 11 Pipe 1 Inch (3m)")
                    .slug("astral-cpvc-pro-sdr11-pipe-1inch")
                    .sku("PLM-CPVC-1IN-SDR11")
                    .brand("Astral")
                    .description("Hot and cold water CPVC plumbing pipe conforming to ASTM D2846.")
                    .imageUrl("https://cdn.hinchmart.com/products/cpvc_pipe.jpg")
                    .price(BigDecimal.valueOf(540.0))
                    .mrp(BigDecimal.valueOf(620.0))
                    .unit("Pieces")
                    .moq(20)
                    .stockQty(800)
                    .active(true)
                    .is24HourDelivery(true)
                    .rating(4.8)
                    .reviewCount(19)
                    .gstRate(BigDecimal.valueOf(18.0))
                    .hsnCode("3917")
                    .build();

            productRepository.saveAll(List.of(p1, p2, p3, p4));
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
