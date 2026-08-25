package com.example.project.customer.controller;

import com.example.project.customer.dto.CustomerDocumentRequest;
import com.example.project.customer.dto.CustomerDocumentResponse;
import com.example.project.customer.dto.RejectDocumentRequest;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.CustomerNotFoundException;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.service.CustomerDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerDocumentController.class)
@Import(GlobalExceptionHandler.class)
class CustomerDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerDocumentService documentService;

    @Test
    @DisplayName("POST /api/customers/{customerId}/documents - Should submit document and return 201 Created")
    void submitDocument_Success() throws Exception {
        CustomerDocumentRequest request = new CustomerDocumentRequest(
                DocumentType.GST_CERTIFICATE,
                "GST Registration Certificate (Form REG-06)",
                "27AABCV1234E1Z5",
                "GST_27AABCV1234E1Z5.pdf",
                "https://storage.example.com/docs/GST_27AABCV1234E1Z5.pdf",
                "1.4 MB",
                null
        );

        CustomerDocumentResponse response = new CustomerDocumentResponse(
                1,
                1,
                DocumentType.GST_CERTIFICATE,
                "GST Registration Certificate (Form REG-06)",
                "27AABCV1234E1Z5",
                "GST_27AABCV1234E1Z5.pdf",
                "https://storage.example.com/docs/GST_27AABCV1234E1Z5.pdf",
                "1.4 MB",
                VerificationStatus.PENDING,
                null,
                null,
                LocalDateTime.now(),
                null
        );

        when(documentService.submitDocument(eq(1), any(CustomerDocumentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/customers/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.documentType").value("GST_CERTIFICATE"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fileName").value("GST_27AABCV1234E1Z5.pdf"));
    }

    @Test
    @DisplayName("POST /api/customers/{customerId}/documents - Should return 400 when title is blank")
    void submitDocument_InvalidRequest_BlankTitle() throws Exception {
        CustomerDocumentRequest request = new CustomerDocumentRequest(
                DocumentType.GST_CERTIFICATE,
                "",
                "27AABCV1234E1Z5",
                "GST.pdf",
                "https://storage.example.com/GST.pdf",
                "1.4 MB",
                null
        );

        mockMvc.perform(post("/api/customers/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Title must not be blank"));
    }

    @Test
    @DisplayName("GET /api/customers/{customerId}/documents - Should return list of documents")
    void getCustomerDocuments_Success() throws Exception {
        CustomerDocumentResponse doc1 = new CustomerDocumentResponse(
                1, 1, DocumentType.GST_CERTIFICATE, "GST Cert", "27A", "gst.pdf", "url1", "1.4 MB",
                VerificationStatus.VERIFIED, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(documentService.getDocumentsByCustomerId(1, null)).thenReturn(List.of(doc1));

        mockMvc.perform(get("/api/customers/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documentId").value(1))
                .andExpect(jsonPath("$[0].status").value("VERIFIED"));
    }

    @Test
    @DisplayName("GET /api/documents/{documentId} - Should return document by ID")
    void getDocumentById_Success() throws Exception {
        CustomerDocumentResponse doc = new CustomerDocumentResponse(
                1, 1, DocumentType.TRADE_LICENSE, "Trade License", "TL-1", "trade.pdf", "url", "1.1 MB",
                VerificationStatus.VERIFIED, null, LocalDate.of(2027, 3, 31), LocalDateTime.now(), LocalDateTime.now()
        );

        when(documentService.getDocumentById(1)).thenReturn(doc);

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.documentType").value("TRADE_LICENSE"))
                .andExpect(jsonPath("$.expiresOn").value("2027-03-31"));
    }

    @Test
    @DisplayName("PATCH /api/documents/{documentId}/verify - Should mark document as VERIFIED")
    void verifyDocument_Success() throws Exception {
        CustomerDocumentResponse verified = new CustomerDocumentResponse(
                1, 1, DocumentType.GST_CERTIFICATE, "GST Cert", "27A", "gst.pdf", "url1", "1.4 MB",
                VerificationStatus.VERIFIED, null, null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(documentService.verifyDocument(1)).thenReturn(verified);

        mockMvc.perform(patch("/api/documents/1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/documents/{documentId}/reject - Should mark document as REJECTED with reason")
    void rejectDocument_Success() throws Exception {
        RejectDocumentRequest rejectReq = new RejectDocumentRequest("Image blurry");

        CustomerDocumentResponse rejected = new CustomerDocumentResponse(
                1, 1, DocumentType.GST_CERTIFICATE, "GST Cert", "27A", "gst.pdf", "url1", "1.4 MB",
                VerificationStatus.REJECTED, "Image blurry", null, LocalDateTime.now(), null
        );

        when(documentService.rejectDocument(1, "Image blurry")).thenReturn(rejected);

        mockMvc.perform(patch("/api/documents/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Image blurry"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{documentId} - Should delete document and return 204")
    void deleteDocument_Success() throws Exception {
        doNothing().when(documentService).deleteDocument(1);

        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(1);
    }
}
