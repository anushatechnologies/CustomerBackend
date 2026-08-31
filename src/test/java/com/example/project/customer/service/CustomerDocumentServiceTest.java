package com.example.project.customer.service;

import com.example.project.customer.dto.CustomerDocumentRequest;
import com.example.project.customer.dto.CustomerDocumentResponse;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.CustomerDocument;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.repository.CustomerDocumentRepository;
import com.example.project.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDocumentServiceTest {

    @Mock
    private CustomerDocumentRepository documentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private CustomerDocumentServiceImpl documentService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setName("Aparna Enterprise");
        customer.setEmail("contact@aparna.com");
        customer.setPhone("+919876543210");
    }

    @Test
    @DisplayName("submitDocument - Should create new document for customer")
    void submitDocument_Success() {
        CustomerDocumentRequest request = new CustomerDocumentRequest(
                DocumentType.GST_CERTIFICATE,
                "GST Registration",
                "27AABCV1234E1Z5",
                "gst.pdf",
                "https://storage/gst.pdf",
                "1.4 MB",
                null
        );

        CustomerDocument savedDoc = new CustomerDocument();
        savedDoc.setDocumentId(10);
        savedDoc.setCustomer(customer);
        savedDoc.setDocumentType(DocumentType.GST_CERTIFICATE);
        savedDoc.setTitle("GST Registration");
        savedDoc.setDocumentNumber("27AABCV1234E1Z5");
        savedDoc.setFileName("gst.pdf");
        savedDoc.setFileUrl("https://storage/gst.pdf");
        savedDoc.setFileSize("1.4 MB");
        savedDoc.setStatus(VerificationStatus.PENDING);
        savedDoc.setUploadedAt(LocalDateTime.now());

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(documentRepository.findByCustomer_CustomerIdAndDocumentType(1, DocumentType.GST_CERTIFICATE))
                .thenReturn(Optional.empty());
        when(documentRepository.save(any(CustomerDocument.class))).thenReturn(savedDoc);

        CustomerDocumentResponse response = documentService.submitDocument(1, request);

        assertThat(response).isNotNull();
        assertThat(response.documentId()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.documentType()).isEqualTo(DocumentType.GST_CERTIFICATE);
    }

    @Test
    @DisplayName("submitDocument - Should throw CustomerNotFoundException when customer does not exist")
    void submitDocument_CustomerNotFound() {
        CustomerDocumentRequest request = new CustomerDocumentRequest(
                DocumentType.GST_CERTIFICATE, "Title", null, "file.pdf", "url", null, null
        );

        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.submitDocument(99, request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found with id: 99");
    }

    @Test
    @DisplayName("verifyDocument - Should set status to VERIFIED and timestamp verifiedAt")
    void verifyDocument_Success() {
        CustomerDocument doc = new CustomerDocument();
        doc.setDocumentId(1);
        doc.setCustomer(customer);
        doc.setDocumentType(DocumentType.GST_CERTIFICATE);
        doc.setTitle("GST Registration");
        doc.setFileName("gst.pdf");
        doc.setFileUrl("url");
        doc.setStatus(VerificationStatus.PENDING);
        doc.setUploadedAt(LocalDateTime.now());

        when(documentRepository.findById(1)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(CustomerDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerDocumentResponse response = documentService.verifyDocument(1);

        assertThat(response.status()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(response.verifiedAt()).isNotNull();
        assertThat(response.rejectionReason()).isNull();
    }

    @Test
    @DisplayName("rejectDocument - Should set status to REJECTED with reason")
    void rejectDocument_Success() {
        CustomerDocument doc = new CustomerDocument();
        doc.setDocumentId(1);
        doc.setCustomer(customer);
        doc.setDocumentType(DocumentType.GST_CERTIFICATE);
        doc.setTitle("GST Registration");
        doc.setFileName("gst.pdf");
        doc.setFileUrl("url");
        doc.setStatus(VerificationStatus.PENDING);
        doc.setUploadedAt(LocalDateTime.now());

        when(documentRepository.findById(1)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(CustomerDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerDocumentResponse response = documentService.rejectDocument(1, "Invalid document");

        assertThat(response.status()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Invalid document");
        assertThat(response.verifiedAt()).isNull();
    }

    @Test
    @DisplayName("deleteDocument - Should delete existing document and clean up S3 file")
    void deleteDocument_Success() {
        CustomerDocument doc = new CustomerDocument();
        doc.setDocumentId(1);
        doc.setCustomer(customer);
        doc.setFileUrl("https://storage/documents/file.pdf");

        when(documentRepository.findById(1)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(1);

        verify(documentRepository).delete(doc);
        verify(s3ImageService).deleteImage("https://storage/documents/file.pdf");
    }

    @Test
    @DisplayName("submitDocument - Should delete old S3 file when replacing existing document")
    void submitDocument_ReplacesOldS3File() {
        CustomerDocument existingDoc = new CustomerDocument();
        existingDoc.setDocumentId(10);
        existingDoc.setCustomer(customer);
        existingDoc.setDocumentType(DocumentType.GST_CERTIFICATE);
        existingDoc.setFileUrl("https://storage/documents/old-gst.pdf");

        CustomerDocumentRequest request = new CustomerDocumentRequest(
                DocumentType.GST_CERTIFICATE,
                "New GST",
                "27AABCV1234E1Z5",
                "new-gst.pdf",
                "https://storage/documents/new-gst.pdf",
                "2.0 MB",
                null
        );

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(documentRepository.findByCustomer_CustomerIdAndDocumentType(1, DocumentType.GST_CERTIFICATE))
                .thenReturn(Optional.of(existingDoc));
        when(documentRepository.save(any(CustomerDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.submitDocument(1, request);

        verify(s3ImageService).deleteImage("https://storage/documents/old-gst.pdf");
    }
}
