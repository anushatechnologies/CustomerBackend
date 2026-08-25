package com.example.project.customer.seller;

import com.example.project.customer.seller.dto.UpdateSellerProfileRequest;
import com.example.project.customer.seller.dto.SellerProfileResponse;
import com.example.project.customer.seller.dto.CreateSellerDocumentRequest;
import com.example.project.customer.seller.dto.SellerDocumentResponse;
import com.example.project.customer.seller.dto.SubmitVerificationResponse;
import com.example.project.customer.seller.dto.SellerVerificationStatusResponse;
import com.example.project.customer.seller.entity.Address;
import com.example.project.customer.seller.entity.LegalInfo;
import com.example.project.customer.seller.entity.Seller;
import com.example.project.customer.seller.entity.SellerDocument;
import com.example.project.customer.seller.entity.VerificationStatus;
import com.example.project.customer.seller.repository.SellerDocumentRepository;
import com.example.project.customer.seller.repository.SellerRepository;
import com.example.project.customer.seller.service.SellerService;
import com.example.project.customer.seller.service.SellerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository sellerDocumentRepository;

    @InjectMocks
    private SellerServiceImpl sellerService;

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
    }

    @Test
    void getProfile_shouldReturnSellerProfile() {
        when(sellerRepository.findById("seller_101")).thenReturn(Optional.of(seller));

        SellerProfileResponse response = sellerService.getSellerProfile("seller_101");

        assertEquals("seller_101", response.getId());
        assertEquals("Ultratech Infra & Steel Suppliers LLP", response.getCompanyName());
        assertEquals(92, response.getCompletionPercentage());
        assertEquals(VerificationStatus.PENDING, response.getVerificationStatus());
    }

    @Test
    void updateProfile_shouldPersistUpdatedValues() {
        UpdateSellerProfileRequest request = new UpdateSellerProfileRequest();
        request.setDescription("Updated description");
        request.setCountry("India");
        request.setState("Maharashtra");
        request.setCity("Bhiwandi");
        request.setPincode("421302");
        request.setCompleteAddress("Updated address");
        request.setGstin("27AABCV1234E1Z5");
        request.setPan("AABCV1234E");
        request.setCin("U45200MH2014LLP123456");
        request.setMsme("UDYAM-MH-33-0098762");
        request.setServiceAreas(List.of("Maharashtra", "Gujarat"));

        when(sellerRepository.findById("seller_101")).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerProfileResponse response = sellerService.updateSellerProfile("seller_101", request);

        assertEquals("Updated description", response.getDescription());
        assertEquals("Updated address", response.getAddress().getCompleteAddress());
        verify(sellerRepository).save(seller);
    }

    @Test
    void createDocument_shouldPersistDocumentMetadata() {
        CreateSellerDocumentRequest request = new CreateSellerDocumentRequest();
        request.setDocumentType("GST_CERTIFICATE");
        request.setFileName("gst_certificate.pdf");
        request.setFileUrl("https://cdn.example.com/seller/documents/gst_certificate.pdf");
        request.setFileType("application/pdf");

        when(sellerRepository.findById("seller_101")).thenReturn(Optional.of(seller));
        when(sellerDocumentRepository.existsBySellerIdAndDocumentType("seller_101", "GST_CERTIFICATE")).thenReturn(false);
        when(sellerDocumentRepository.save(any(SellerDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerDocumentResponse response = sellerService.createDocument("seller_101", request);

        assertEquals("GST_CERTIFICATE", response.getDocumentType());
        assertEquals("gst_certificate.pdf", response.getFileName());
        assertEquals("application/pdf", response.getFileType());
    }

    @Test
    void submitVerification_shouldMoveToUnderReviewWhenValid() {
        when(sellerRepository.findById("seller_101")).thenReturn(Optional.of(seller));
        when(sellerDocumentRepository.findBySellerIdAndDocumentType("seller_101", "GST_CERTIFICATE")).thenReturn(Optional.of(new SellerDocument()));
        when(sellerDocumentRepository.findBySellerIdAndDocumentType("seller_101", "PAN")).thenReturn(Optional.of(new SellerDocument()));
        when(sellerDocumentRepository.findBySellerIdAndDocumentType("seller_101", "MSME")).thenReturn(Optional.of(new SellerDocument()));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubmitVerificationResponse response = sellerService.submitForVerification("seller_101");

        assertEquals(VerificationStatus.UNDER_REVIEW, response.getVerificationStatus());
        assertNotNull(response.getSubmittedAt());
    }

    @Test
    void getVerificationStatus_shouldIncludeChecklistAndCompletion() {
        seller.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
        seller.setSubmittedAt(Instant.now());
        when(sellerRepository.findById("seller_101")).thenReturn(Optional.of(seller));
        when(sellerDocumentRepository.findBySellerIdAndDocumentType("seller_101", "GST_CERTIFICATE")).thenReturn(Optional.of(new SellerDocument()));
        when(sellerDocumentRepository.findBySellerIdAndDocumentType("seller_101", "PAN")).thenReturn(Optional.of(new SellerDocument()));
        when(sellerDocumentRepository.findBySellerIdAndDocumentType("seller_101", "MSME")).thenReturn(Optional.of(new SellerDocument()));

        SellerVerificationStatusResponse response = sellerService.getVerificationStatus("seller_101");

        assertEquals(VerificationStatus.UNDER_REVIEW, response.getVerificationStatus());
        assertTrue(response.getChecklist().isCompanyProfile());
        assertTrue(response.getChecklist().isGstin());
        assertNotNull(response.getSubmittedAt());
    }
}
