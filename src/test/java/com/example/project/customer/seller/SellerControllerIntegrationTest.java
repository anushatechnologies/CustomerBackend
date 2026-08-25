package com.example.project.customer.seller;

import com.example.project.customer.seller.entity.Seller;
import com.example.project.customer.seller.entity.VerificationStatus;
import com.example.project.customer.seller.repository.SellerRepository;
import com.example.project.customer.seller.repository.SellerDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private SellerDocumentRepository sellerDocumentRepository;

    private Seller seller;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setId("seller_101");
        seller.setCompanyName("Ultratech Infra & Steel Suppliers LLP");
        seller.setBusinessType("Distributor");
        seller.setDescription("Authorized wholesale stockist");
        seller.setCountry("India");
        seller.setState("Maharashtra");
        seller.setCity("Bhiwandi");
        seller.setPincode("421302");
        seller.setCompleteAddress("Plot C-14, Mankoli Industrial Hub");
        seller.setGstin("27AABCV1234E1Z5");
        seller.setPan("AABCV1234E");
        seller.setCin("U45200MH2014LLP123456");
        seller.setMsme("UDYAM-MH-33-0098762");
        seller.setServiceAreas(List.of("Maharashtra", "Gujarat", "Goa"));
        seller.setVerificationStatus(VerificationStatus.PENDING);
        sellerRepository.save(seller);
    }

    @Test
    void getSellerProfile_shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/seller/profile").header("Authorization", "Bearer seller_101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("seller_101"))
                .andExpect(jsonPath("$.companyName").value("Ultratech Infra & Steel Suppliers LLP"));
    }

    @Test
    void putSellerProfile_shouldUpdateProfile() throws Exception {
        String requestBody = "{\"description\":\"Updated description\",\"country\":\"India\",\"state\":\"Maharashtra\",\"city\":\"Bhiwandi\",\"pincode\":\"421302\",\"completeAddress\":\"Updated address\",\"gstin\":\"27AABCV1234E1Z5\",\"pan\":\"AABCV1234E\",\"cin\":\"U45200MH2014LLP123456\",\"msme\":\"UDYAM-MH-33-0098762\",\"serviceAreas\":[\"Maharashtra\",\"Gujarat\"]}";

        mockMvc.perform(put("/seller/profile").header("Authorization", "Bearer seller_101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void createDocument_shouldPersistRecord() throws Exception {
        String requestBody = "{\"documentType\":\"GST_CERTIFICATE\",\"fileName\":\"gst_certificate.pdf\",\"fileUrl\":\"https://cdn.example.com/seller/documents/gst_certificate.pdf\",\"fileType\":\"application/pdf\"}";

        mockMvc.perform(post("/seller/documents").header("Authorization", "Bearer seller_101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("GST_CERTIFICATE"));
    }

    @Test
    void getVerificationStatus_shouldReturnChecklist() throws Exception {
        mockMvc.perform(get("/seller/verification/status").header("Authorization", "Bearer seller_101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }
}
