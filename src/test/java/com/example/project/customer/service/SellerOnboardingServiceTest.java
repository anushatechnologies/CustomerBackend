package com.example.project.customer.service;

import com.example.project.customer.dto.BankDetailsRequest;
import com.example.project.customer.dto.BusinessTaxRequest;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.dto.PersonalKycRequest;
import com.example.project.customer.dto.SellerOnboardingSummaryResponse;
import com.example.project.customer.entity.BusinessType;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.OnboardingStatus;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.SellerDocument;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.SellerDocumentRepository;
import com.example.project.customer.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SellerOnboardingServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository documentRepository;

    @Mock
    private S3ImageService s3ImageService;

    private SellerOnboardingServiceImpl onboardingService;

    private Seller seller;

    @BeforeEach
    void setUp() {
        jakarta.validation.Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        onboardingService = new SellerOnboardingServiceImpl(sellerRepository, documentRepository, s3ImageService, validator);

        seller = Seller.builder()
                .sellerId(1)
                .name("Rajesh Sharma")
                .email("rajesh@company.com")
                .phone("+917661966947")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .onboardingStatus(OnboardingStatus.STEP_1)
                .build();
    }

    @Test
    @DisplayName("savePersonalKyc - Success creating new seller")
    void savePersonalKyc_NewSeller_Success() {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "rajesh@company.com", "+917661966947", "ABCDE1234F", "123456789012");
        when(sellerRepository.findFirstByEmailIgnoreCase("rajesh@company.com")).thenReturn(Optional.empty());
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> {
            Seller s = i.getArgument(0);
            s.setSellerId(1);
            return s;
        });

        Seller saved = onboardingService.savePersonalKyc(req);

        assertThat(saved.getSellerId()).isEqualTo(1);
        assertThat(saved.getName()).isEqualTo("Rajesh Sharma");
        assertThat(saved.getEmail()).isEqualTo("rajesh@company.com");
        assertThat(saved.getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(saved.getAadhaarNumber()).isEqualTo("123456789012");
        assertThat(saved.getOnboardingStatus()).isEqualTo(OnboardingStatus.STEP_1);
    }

    @Test
    @DisplayName("savePersonalKyc - Success with PAN card file upload")
    void savePersonalKyc_WithFile_Success() {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "rajesh@company.com", "+917661966947", "ABCDE1234F", "123456789012");
        MockMultipartFile file = new MockMultipartFile("panCardFile", "pan.png", "image/png", "dummy".getBytes());

        when(sellerRepository.findFirstByEmailIgnoreCase("rajesh@company.com")).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);
        when(documentRepository.findBySellerIdAndDocumentType(1, DocumentType.PAN)).thenReturn(Optional.empty());
        when(s3ImageService.uploadImage(any(), any(String.class))).thenReturn(ImageUploadResponse.builder().fileUrl("https://s3.aws.com/pan.png").build());

        Seller saved = onboardingService.savePersonalKyc(req, file);

        assertThat(saved.getSellerId()).isEqualTo(1);
        verify(documentRepository).save(any(SellerDocument.class));
    }

    @Test
    @DisplayName("savePersonalKyc - Throws IllegalArgumentException when request is null")
    void savePersonalKyc_NullRequest_ThrowsException() {
        assertThatThrownBy(() -> onboardingService.savePersonalKyc(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("saveBusinessTax - Success updating business details")
    void saveBusinessTax_Success() {
        BusinessTaxRequest req = new BusinessTaxRequest("Rajesh Trading Co", BusinessType.WHOLESALER, "29ABCDE1234F1Z5", "123 MG Road", "Karnataka", "Bangalore", "560001");
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller saved = onboardingService.saveBusinessTax(1, req);

        assertThat(saved.getCompanyName()).isEqualTo("Rajesh Trading Co");
        assertThat(saved.getBusinessType()).isEqualTo(BusinessType.WHOLESALER);
        assertThat(saved.getGstin()).isEqualTo("29ABCDE1234F1Z5");
        assertThat(saved.getBusinessAddress()).isEqualTo("123 MG Road");
        assertThat(saved.getState()).isEqualTo("Karnataka");
        assertThat(saved.getCity()).isEqualTo("Bangalore");
        assertThat(saved.getPincode()).isEqualTo("560001");
        assertThat(saved.getOnboardingStatus()).isEqualTo(OnboardingStatus.STEP_2);
    }


    @Test
    @DisplayName("saveBusinessTax - Throws ResourceNotFoundException for invalid sellerId")
    void saveBusinessTax_NotFound_ThrowsException() {
        BusinessTaxRequest req = new BusinessTaxRequest("Rajesh Trading Co", BusinessType.WHOLESALER, "29ABCDE1234F1Z5", "123 MG Road", "Karnataka", "Bangalore", "560001");
        when(sellerRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.saveBusinessTax(999, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Seller not found with id: 999");
    }

    @Test
    @DisplayName("saveBankDetails - Success when account numbers match")
    void saveBankDetails_Success() {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "50100234567890", "50100234567890", "HDFC0000123", "CURRENT");
        seller.setOnboardingStatus(OnboardingStatus.STEP_2);
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller saved = onboardingService.saveBankDetails(1, req);

        assertThat(saved.getBankName()).isEqualTo("HDFC Bank");
        assertThat(saved.getAccountHolderName()).isEqualTo("Rajesh Sharma");
        assertThat(saved.getAccountNumber()).isEqualTo("50100234567890");
        assertThat(saved.getIfscCode()).isEqualTo("HDFC0000123");
        assertThat(saved.getAccountType()).isEqualTo("CURRENT");
        assertThat(saved.getOnboardingStatus()).isEqualTo(OnboardingStatus.STEP_3);
    }

    @Test
    @DisplayName("saveBankDetails - Throws ResourceConflictException on account number mismatch")
    void saveBankDetails_Mismatch_ThrowsException() {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "50100234567890", "50100234567899", "HDFC0000123", "CURRENT");
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> onboardingService.saveBankDetails(1, req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Account number and Confirm Account Number do not match");
    }

    @Test
    @DisplayName("getSummary - Success with complete details and masked account number")
    void getSummary_Complete_Success() {
        seller.setCompanyName("Rajesh Trading Co");
        seller.setBusinessType(BusinessType.WHOLESALER);
        seller.setGstin("29ABCDE1234F1Z5");
        seller.setBusinessAddress("123 MG Road");
        seller.setState("Karnataka");
        seller.setCity("Bangalore");
        seller.setPincode("560001");
        seller.setBankName("HDFC Bank");
        seller.setAccountHolderName("Rajesh Sharma");
        seller.setAccountNumber("50100234567890");
        seller.setIfscCode("HDFC0000123");
        seller.setAccountType("CURRENT");

        SellerDocument doc = new SellerDocument();
        doc.setId(10L);
        doc.setSellerId(1);
        doc.setDocumentType(DocumentType.PAN);
        doc.setFileName("pan.png");
        doc.setFileUrl("https://s3.aws.com/pan.png");
        doc.setVerificationStatus(VerificationStatus.PENDING);

        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(documentRepository.findBySellerId(1)).thenReturn(List.of(doc));

        SellerOnboardingSummaryResponse summary = onboardingService.getSummary(1);

        assertThat(summary.sellerId()).isEqualTo(1);
        assertThat(summary.personalDetails().isComplete()).isTrue();
        assertThat(summary.businessTaxDetails().isComplete()).isTrue();
        assertThat(summary.bankDetails().isComplete()).isTrue();
        assertThat(summary.bankDetails().maskedAccountNumber()).isEqualTo("XXXXXXXXXX7890");
        assertThat(summary.documents()).hasSize(1);
        assertThat(summary.isReadyForSubmission()).isTrue();
    }

    @Test
    @DisplayName("getSummary - Partial data results in isReadyForSubmission = false")
    void getSummary_Incomplete_ReturnsFalse() {
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(documentRepository.findBySellerId(1)).thenReturn(List.of());

        SellerOnboardingSummaryResponse summary = onboardingService.getSummary(1);

        assertThat(summary.personalDetails().isComplete()).isTrue();
        assertThat(summary.businessTaxDetails().isComplete()).isFalse();
        assertThat(summary.bankDetails().isComplete()).isFalse();
        assertThat(summary.isReadyForSubmission()).isFalse();
    }

    @Test
    @DisplayName("finalSubmit - Success when all mandatory fields are complete")
    void finalSubmit_Success() {
        seller.setCompanyName("Rajesh Trading Co");
        seller.setBusinessType(BusinessType.WHOLESALER);
        seller.setGstin("29ABCDE1234F1Z5");
        seller.setBusinessAddress("123 MG Road");
        seller.setState("Karnataka");
        seller.setCity("Bangalore");
        seller.setPincode("560001");
        seller.setBankName("HDFC Bank");
        seller.setAccountHolderName("Rajesh Sharma");
        seller.setAccountNumber("50100234567890");
        seller.setIfscCode("HDFC0000123");
        seller.setAccountType("CURRENT");

        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));

        Seller submitted = onboardingService.finalSubmit(1);

        assertThat(submitted.getOnboardingStatus()).isEqualTo(OnboardingStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("finalSubmit - Throws IllegalStateException when fields are incomplete")
    void finalSubmit_Incomplete_ThrowsException() {
        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> onboardingService.finalSubmit(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot submit for verification: All mandatory personal, business, and bank details must be completed.");
    }

    @Test
    @DisplayName("savePersonalKyc - Throws ResourceConflictException when Mobile Number is already registered")
    void savePersonalKyc_DuplicatePhone_ThrowsConflict() {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "new@company.com", "+917661966947", "ABCDE1234F", "123456789012");
        Seller otherSeller = Seller.builder().sellerId(2).phone("+917661966947").build();

        when(sellerRepository.findFirstByEmailIgnoreCase("new@company.com")).thenReturn(Optional.empty());
        when(sellerRepository.findAllByPhone("+917661966947")).thenReturn(List.of(otherSeller));

        assertThatThrownBy(() -> onboardingService.savePersonalKyc(req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Mobile number '+917661966947' is already registered");
    }

    @Test
    @DisplayName("savePersonalKyc - Throws ResourceConflictException when PAN is already registered")
    void savePersonalKyc_DuplicatePan_ThrowsConflict() {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "new@company.com", "+917661966947", "ABCDE1234F", "123456789012");
        Seller otherSeller = Seller.builder().sellerId(2).panNumber("ABCDE1234F").build();

        when(sellerRepository.findFirstByEmailIgnoreCase("new@company.com")).thenReturn(Optional.empty());
        when(sellerRepository.findAllByPhone("+917661966947")).thenReturn(List.of());
        when(sellerRepository.findAllByPanNumber("ABCDE1234F")).thenReturn(List.of(otherSeller));

        assertThatThrownBy(() -> onboardingService.savePersonalKyc(req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("PAN number 'ABCDE1234F' is already registered");
    }

    @Test
    @DisplayName("savePersonalKyc - Throws ResourceConflictException when Aadhaar is already registered")
    void savePersonalKyc_DuplicateAadhaar_ThrowsConflict() {
        PersonalKycRequest req = new PersonalKycRequest("Rajesh Sharma", "new@company.com", "+917661966947", "ABCDE1234F", "123456789012");
        Seller otherSeller = Seller.builder().sellerId(2).aadhaarNumber("123456789012").build();

        when(sellerRepository.findFirstByEmailIgnoreCase("new@company.com")).thenReturn(Optional.empty());
        when(sellerRepository.findAllByPhone("+917661966947")).thenReturn(List.of());
        when(sellerRepository.findAllByPanNumber("ABCDE1234F")).thenReturn(List.of());
        when(sellerRepository.findAllByAadhaarNumber("123456789012")).thenReturn(List.of(otherSeller));

        assertThatThrownBy(() -> onboardingService.savePersonalKyc(req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Aadhaar number '123456789012' is already registered");
    }

    @Test
    @DisplayName("saveBusinessTax - Throws ResourceConflictException when GSTIN is already registered")
    void saveBusinessTax_DuplicateGstin_ThrowsConflict() {
        BusinessTaxRequest req = new BusinessTaxRequest("Rajesh Trading Co", BusinessType.WHOLESALER, "29ABCDE1234F1Z5", "123 MG Road", "Karnataka", "Bangalore", "560001");
        Seller otherSeller = Seller.builder().sellerId(2).gstin("29ABCDE1234F1Z5").build();

        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.findAllByGstin("29ABCDE1234F1Z5")).thenReturn(List.of(otherSeller));

        assertThatThrownBy(() -> onboardingService.saveBusinessTax(1, req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("GSTIN '29ABCDE1234F1Z5' is already registered");
    }

    @Test
    @DisplayName("saveBankDetails - Throws ResourceConflictException when Account Number is already registered")
    void saveBankDetails_DuplicateAccountNumber_ThrowsConflict() {
        BankDetailsRequest req = new BankDetailsRequest("HDFC Bank", "Rajesh Sharma", "50100234567890", "50100234567890", "HDFC0000123", "CURRENT");
        Seller otherSeller = Seller.builder().sellerId(2).accountNumber("50100234567890").build();

        when(sellerRepository.findById(1)).thenReturn(Optional.of(seller));
        when(sellerRepository.findAllByAccountNumber("50100234567890")).thenReturn(List.of(otherSeller));

        assertThatThrownBy(() -> onboardingService.saveBankDetails(1, req))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Bank account number '50100234567890' is already registered");
    }
}
