package com.example.project.customer;

import com.example.project.customer.dto.*;
import com.example.project.customer.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RfqAndUserProfileFlowTest {

    @Autowired
    private RfqService rfqService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private ProductService productService;

    @Test
    void testRfqLifecycleAndAcceptQuotation() {
        Integer userId = 101;

        // 1. Submit RFQ
        RfqRequest req = new RfqRequest();
        req.setTitle("Bulk Procurement for G+14 Commercial Tower Project");
        req.setCategory("Steel & TMT");
        req.setProductMaterial("TMT Rebars Fe 550D");
        req.setQuantity(100);
        req.setUnit("MT");
        req.setTechnicalGrade("Fe 550D Primary (Tata/JSW/Jindal)");
        req.setMtcRequired(true);
        req.setDeliveryLocation("Financial District, Gachibowli, Hyderabad - 500032");
        req.setRequiredByDate(LocalDate.now().plusDays(15));
        req.setSiteAccess("Heavy Trailer Access Available");
        req.setCraneRequired(true);
        req.setTargetBudget(new BigDecimal("5100000.00"));
        req.setPaymentTerms("LETTER_OF_CREDIT");
        req.setSpecifications("Only secondary bend test certified rebars accepted.");
        req.setBoqAttachmentUrl("https://cdn.hinchmart.com/rfq_docs/boq_project_tower_b.pdf");

        RfqResponse rfq = rfqService.createRfq(userId, req);
        assertNotNull(rfq);
        assertNotNull(rfq.getRfqId());
        assertNotNull(rfq.getRfqNumber());
        assertEquals("OPEN", rfq.getStatus());

        // 2. Add question
        RfqQuestionDto question = rfqService.addQuestion(userId, rfq.getRfqId(),
                new RfqQuestionRequest("Can the 100 MT consignment be delivered in 4 weekly batches?"));
        assertNotNull(question);
        assertEquals("PENDING", question.getStatus());

        // 3. Test pre-seeded RFQ and accept quotation
        var rfqs = rfqService.getRfqs(userId, "OPEN", 1, 10);
        assertNotNull(rfqs);
        assertFalse(rfqs.getData().isEmpty());
        RfqResponse seedRfq = rfqs.getData().get(0);

        List<RfqQuotationDto> quotes = rfqService.getQuotations(userId, seedRfq.getRfqId());
        if (!quotes.isEmpty()) {
            RfqQuotationDto quote = quotes.get(0);
            AcceptQuotationResponse acceptResp = rfqService.acceptQuotation(userId, quote.getQuoteId());
            assertNotNull(acceptResp);
            assertNotNull(acceptResp.getOrderId());
            assertNotNull(acceptResp.getOrderNumber());
            assertEquals("CONVERTED_TO_ORDER", acceptResp.getStatus());
        }
    }

    @Test
    void testUserProfileAndProcurementStats() {
        Integer userId = 101;
        UserProfileResponse profile = userProfileService.getProfile(userId);
        assertNotNull(profile);
        assertEquals("Rajesh Sharma", profile.getFullName());
        assertEquals("BUYER", profile.getRole());
        assertEquals("GOLD", profile.getTier());
        assertNotNull(profile.getProcurementStats());
        assertNotNull(profile.getBusiness());
        assertEquals("Apex Infra Projects Pvt Ltd", profile.getBusiness().getCompanyName());
    }

    @Test
    void testWishlist() {
        Integer userId = 101;
        var products = productService.getProducts(null, null, null, null, null, null, null, null, 1, 5);
        if (!products.getData().isEmpty()) {
            Integer prodId = products.getData().get(0).getProductId();
            wishlistService.addToWishlist(userId, prodId);
            List<ProductResponse> wishlist = wishlistService.getWishlist(userId);
            assertNotNull(wishlist);
            assertFalse(wishlist.isEmpty());

            wishlistService.removeFromWishlist(userId, prodId);
        }
    }
}
