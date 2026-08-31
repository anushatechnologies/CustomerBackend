package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.PersonalKycRequest;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.BusinessType;
import com.example.project.customer.entity.OnboardingStatus;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.service.SellerOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerOnboardingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@SuppressWarnings("null")
class SellerOnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SellerOnboardingService onboardingService;

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal (JSON) - Success")
    void step1_Personal_Json_Success() throws Exception {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "rajesh@company.com", "+917661966947", "ABCDE1234F", "123456789012");
        Seller seller = Seller.builder()
                .sellerId(1)
                .name("Rajesh Sharma")
                .email("rajesh@company.com")
                .phone("+917661966947")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .onboardingStatus(OnboardingStatus.STEP_1)
                .build();

        when(onboardingService.savePersonalKyc(any(PersonalKycRequest.class))).thenReturn(seller);

        mockMvc.perform(post("/api/sellers/onboarding/step1-personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerId").value(1))
                .andExpect(jsonPath("$.name").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.email").value("rajesh@company.com"))
                .andExpect(jsonPath("$.panNumber").value("ABCDE1234F"))
                .andExpect(jsonPath("$.aadhaarNumber").value("123456789012"))
                .andExpect(jsonPath("$.onboardingStatus").value("STEP_1"));
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal (Multipart) - Success")
    void step1_Personal_Multipart_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("panCardFile", "pan.png", "image/png", "dummy".getBytes());
        Seller seller = Seller.builder()
                .sellerId(1)
                .name("Rajesh Sharma")
                .email("rajesh@company.com")
                .onboardingStatus(OnboardingStatus.STEP_1)
                .build();

        when(onboardingService.savePersonalKyc(any(PersonalKycRequest.class), any())).thenReturn(seller);

        mockMvc.perform(multipart("/api/sellers/onboarding/step1-personal")
                        .file(file)
                        .param("name", "Rajesh Sharma")
                        .param("email", "rajesh@company.com")
                        .param("phone", "+917661966947")
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarNumber", "123456789012"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerId").value(1));
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal - Edge Case: Blank name returns 400")
    void step1_Personal_BlankName_Returns400() throws Exception {
        PersonalKycRequest req = new PersonalKycRequest("", "rajesh@company.com", "+917661966947", "ABCDE1234F", "123456789012");

        mockMvc.perform(post("/api/sellers/onboarding/step1-personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal - Edge Case: Invalid Email returns 400")
    void step1_Personal_InvalidEmail_Returns400() throws Exception {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "invalid-email-format", "+917661966947", "ABCDE1234F", "123456789012");

        mockMvc.perform(post("/api/sellers/onboarding/step1-personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal - Edge Case: Invalid Phone returns 400")
    void step1_Personal_InvalidPhone_Returns400() throws Exception {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "rajesh@company.com", "123", "ABCDE1234F", "123456789012");

        mockMvc.perform(post("/api/sellers/onboarding/step1-personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal - Edge Case: Invalid PAN returns 400")
    void step1_Personal_InvalidPan_Returns400() throws Exception {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "rajesh@company.com", "+917661966947", "INVALIDPAN", "123456789012");

        mockMvc.perform(post("/api/sellers/onboarding/step1-personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/step1-personal - Edge Case: Invalid Aadhaar (not 12 digits) returns 400")
    void step1_Personal_InvalidAadhaar_Returns400() throws Exception {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "rajesh@company.com", "+917661966947", "ABCDE1234F", "12345");

        mockMvc.perform(post("/api/sellers/onboarding/step1-personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step2-business (JSON) - Success")
    void step2_Business_Json_Success() throws Exception {
        BusinessTaxRequest req = new BusinessTaxRequest("Rajesh Trading Co", BusinessType.WHOLESALER, "29ABCDE1234F1Z5", "123 MG Road", "Karnataka", "Bangalore", "560001");
        Seller seller = Seller.builder()
                .sellerId(1)
                .companyName("Rajesh Trading Co")
                .businessType(BusinessType.WHOLESALER)
                .gstin("29ABCDE1234F1Z5")
                .onboardingStatus(OnboardingStatus.STEP_2)
                .build();

        when(onboardingService.saveBusinessTax(eq(1), any(BusinessTaxRequest.class))).thenReturn(seller);

        mockMvc.perform(post("/api/sellers/onboarding/1/step2-business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Rajesh Trading Co"))
                .andExpect(jsonPath("$.onboardingStatus").value("STEP_2"));
    }


    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step2-business - Edge Case: Invalid GSTIN returns 400")
    void step2_Business_InvalidGstin_Returns400() throws Exception {
        BusinessTaxRequest req = new BusinessTaxRequest("Rajesh Trading Co", BusinessType.WHOLESALER, "INVALID_GST", "123 MG Road", "Karnataka", "Bangalore", "560001");

        mockMvc.perform(post("/api/sellers/onboarding/1/step2-business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step2-business - Edge Case: Invalid Pincode returns 400")
    void step2_Business_InvalidPincode_Returns400() throws Exception {
        BusinessTaxRequest req = new BusinessTaxRequest("Rajesh Trading Co", BusinessType.WHOLESALER, "29ABCDE1234F1Z5", "123 MG Road", "Karnataka", "Bangalore", "123");

        mockMvc.perform(post("/api/sellers/onboarding/1/step2-business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step3-bank - Success")
    void step3_Bank_Success() throws Exception {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "50100234567890", "50100234567890", "HDFC0000123", "CURRENT");
        Seller seller = Seller.builder()
                .sellerId(1)
                .bankName("HDFC Bank")
                .accountHolderName("Rajesh Sharma")
                .onboardingStatus(OnboardingStatus.STEP_3)
                .build();

        when(onboardingService.saveBankDetails(eq(1), any(BankDetailsRequest.class))).thenReturn(seller);

        mockMvc.perform(post("/api/sellers/onboarding/1/step3-bank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("HDFC Bank"))
                .andExpect(jsonPath("$.onboardingStatus").value("STEP_3"));
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step3-bank - Edge Case: Invalid IFSC returns 400")
    void step3_Bank_InvalidIfsc_Returns400() throws Exception {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "50100234567890", "50100234567890", "INVALIDIFSC", "CURRENT");

        mockMvc.perform(post("/api/sellers/onboarding/1/step3-bank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step3-bank - Edge Case: Invalid Account Number returns 400")
    void step3_Bank_InvalidAccountNumber_Returns400() throws Exception {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "123", "123", "HDFC0000123", "CURRENT");

        mockMvc.perform(post("/api/sellers/onboarding/1/step3-bank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/step3-bank - Edge Case: Invalid Account Type returns 400")
    void step3_Bank_InvalidAccountType_Returns400() throws Exception {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "50100234567890", "50100234567890", "HDFC0000123", "FIXED_DEPOSIT");

        mockMvc.perform(post("/api/sellers/onboarding/1/step3-bank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("GET /api/sellers/onboarding/{id}/summary - Success")
    void getSummary_Success() throws Exception {
        var personal = new SellerOnboardingSummaryResponse.PersonalSummary("Rajesh Sharma", "+917661966947", "rajesh@company.com", "ABCDE1234F", "123456789012", true);
        var business = new SellerOnboardingSummaryResponse.BusinessSummary("Rajesh Trading Co", BusinessType.WHOLESALER, "29ABCDE1234F1Z5", "123 MG Road", "Karnataka", "Bangalore", "560001", true);
        var bank = new SellerOnboardingSummaryResponse.BankSummary("HDFC Bank", "Rajesh Sharma", "XXXXXXXXXX7890", "HDFC0000123", "CURRENT", true);

        SellerOnboardingSummaryResponse summary = new SellerOnboardingSummaryResponse(1, OnboardingStatus.STEP_3, personal, business, bank, List.of(), true);

        when(onboardingService.getSummary(1)).thenReturn(summary);

        mockMvc.perform(get("/api/sellers/onboarding/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(1))
                .andExpect(jsonPath("$.personalDetails.name").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.bankDetails.maskedAccountNumber").value("XXXXXXXXXX7890"))
                .andExpect(jsonPath("$.isReadyForSubmission").value(true));
    }

    @Test
    @DisplayName("POST /api/sellers/onboarding/{id}/final-submit - Success")
    void finalSubmit_Success() throws Exception {
        Seller seller = Seller.builder()
                .sellerId(1)
                .onboardingStatus(OnboardingStatus.PENDING_REVIEW)
                .build();

        when(onboardingService.finalSubmit(1)).thenReturn(seller);

        mockMvc.perform(post("/api/sellers/onboarding/1/final-submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingStatus").value("PENDING_REVIEW"));
    }
}
