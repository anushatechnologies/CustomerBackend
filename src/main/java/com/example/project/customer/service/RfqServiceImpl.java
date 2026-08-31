package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.QuotationRequest;
import com.example.project.customer.dto.QuotationResponse;
import com.example.project.customer.dto.RfqQuestionRequest;
import com.example.project.customer.dto.RfqQuestionResponse;
import com.example.project.customer.dto.RfqRequest;
import com.example.project.customer.dto.RfqResponse;
import com.example.project.customer.entity.Order;
import com.example.project.customer.entity.OrderItem;
import com.example.project.customer.entity.Quotation;
import com.example.project.customer.entity.Rfq;
import com.example.project.customer.entity.RfqQuestion;
import com.example.project.customer.entity.TrackingCheckpoint;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.OrderItemRepository;
import com.example.project.customer.repository.OrderRepository;
import com.example.project.customer.repository.QuotationRepository;
import com.example.project.customer.repository.RfqQuestionRepository;
import com.example.project.customer.repository.RfqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("null")
public class RfqServiceImpl implements RfqService {

    private final RfqRepository rfqRepository;
    private final QuotationRepository quotationRepository;
    private final RfqQuestionRepository rfqQuestionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public RfqResponse createRfq(Integer userId, RfqRequest request) {
        int uid = userId != null ? userId : 101;
        String dateYear = String.valueOf(LocalDate.now().getYear());
        long randomSuffix = (long) (Math.random() * 900) + 100;
        String rfqNum = "RFQ-" + dateYear + "-000" + randomSuffix;

        Rfq rfq = Rfq.builder()
                .rfqNumber(rfqNum)
                .userId(uid)
                .title(request.getTitle())
                .category(request.getCategory())
                .productMaterial(request.getProductMaterial())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .technicalGrade(request.getTechnicalGrade())
                .mtcRequired(request.getMtcRequired() != null ? request.getMtcRequired() : true)
                .deliveryLocation(request.getDeliveryLocation())
                .requiredByDate(request.getRequiredByDate())
                .siteAccess(request.getSiteAccess())
                .craneRequired(request.getCraneRequired() != null ? request.getCraneRequired() : false)
                .targetBudget(request.getTargetBudget())
                .paymentTerms(request.getPaymentTerms())
                .specifications(request.getSpecifications())
                .boqAttachmentUrl(request.getBoqAttachmentUrl())
                .status("OPEN")
                .quotesCount(0)
                .build();

        return mapToRfqResponse(rfqRepository.save(rfq));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<RfqResponse>> getRfqs(Integer userId, String status, int page, int limit) {
        int uid = userId != null ? userId : 101;
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Rfq> rfqPage;
        if (status != null && !status.isBlank()) {
            rfqPage = rfqRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
        } else {
            rfqPage = rfqRepository.findByUserIdOrderByCreatedAtDesc(uid, pageable);
        }

        List<RfqResponse> list = rfqPage.getContent().stream().map(this::mapToRfqResponse).toList();
        PaginationMeta pagination = PaginationMeta.of(page > 0 ? page : 1, pageSize, rfqPage.getTotalElements());

        return ApiResponse.paginated(list, pagination);
    }

    @Override
    @Transactional(readOnly = true)
    public RfqResponse getRfqById(Integer id) {
        Rfq rfq = findRfq(id);
        return mapToRfqResponse(rfq);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuotationResponse> getRfqQuotations(Integer rfqId) {
        return quotationRepository.findByRfq_RfqIdOrderByUnitPriceAsc(rfqId).stream()
                .map(this::mapToQuotationResponse).toList();
    }

    @Override
    public QuotationResponse addQuotation(Integer rfqId, QuotationRequest request) {
        Rfq rfq = findRfq(rfqId);

        BigDecimal total = request.getUnitPrice().multiply(BigDecimal.valueOf(rfq.getQuantity()));

        Quotation quotation = Quotation.builder()
                .rfq(rfq)
                .vendorId(request.getVendorId())
                .vendorName(request.getVendorName())
                .unitPrice(request.getUnitPrice())
                .totalAmount(total)
                .deliveryLeadTimeDays(request.getDeliveryLeadTimeDays() != null ? request.getDeliveryLeadTimeDays() : 5)
                .paymentTermsOffered(request.getPaymentTermsOffered())
                .mtcIncluded(request.getMtcIncluded() != null ? request.getMtcIncluded() : true)
                .freightIncluded(request.getFreightIncluded() != null ? request.getFreightIncluded() : true)
                .validUntil(request.getValidUntil() != null ? request.getValidUntil() : LocalDateTime.now().plusDays(7))
                .vendorRating(request.getVendorRating() != null ? request.getVendorRating() : 4.8)
                .status("PENDING")
                .build();

        Quotation saved = quotationRepository.save(quotation);
        rfq.setQuotesCount(quotationRepository.countByRfq_RfqId(rfqId));
        if ("OPEN".equalsIgnoreCase(rfq.getStatus())) {
            rfq.setStatus("QUOTED");
        }
        rfqRepository.save(rfq);

        return mapToQuotationResponse(saved);
    }

    @Override
    public Map<String, Object> acceptQuotation(Integer quoteId) {
        Quotation quote = quotationRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + quoteId));

        Rfq rfq = quote.getRfq();
        quote.setStatus("ACCEPTED");
        quotationRepository.save(quote);

        rfq.setStatus("ACCEPTED");
        rfqRepository.save(rfq);

        // Convert directly to order
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long randomSuffix = (long) (Math.random() * 900) + 100;
        String orderNumber = "ORD-" + dateStr + "-" + randomSuffix;

        BigDecimal total = quote.getTotalAmount();
        BigDecimal taxable = total.divide(BigDecimal.valueOf(1.18), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal gst = total.subtract(taxable);
        BigDecimal halfGst = gst.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(rfq.getUserId())
                .deliveryLocation(rfq.getDeliveryLocation())
                .subtotal(taxable)
                .discount(BigDecimal.ZERO)
                .taxableAmount(taxable)
                .cgst(halfGst)
                .sgst(halfGst)
                .igst(BigDecimal.ZERO)
                .totalGst(gst)
                .freightCharge(BigDecimal.ZERO)
                .totalAmount(total)
                .paymentMethod("LETTER_OF_CREDIT")
                .paymentStatus("PENDING")
                .orderStatus("PLACED")
                .poNumber("PO-RFQ-" + rfq.getRfqId())
                .carrierName(quote.getVendorName() + " Logistics Fleet")
                .vehicleNumber("TS 09 UB 9901")
                .driverName("Driver assigned upon dispatch")
                .trackingNumber("VRL-RFQ-" + rfq.getRfqId())
                .estimatedDelivery(LocalDateTime.now().plusDays(quote.getDeliveryLeadTimeDays() != null ? quote.getDeliveryLeadTimeDays() : 5))
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .productId(0)
                .title(rfq.getProductMaterial() + " (" + (rfq.getTechnicalGrade() != null ? rfq.getTechnicalGrade() : "") + ")")
                .imageUrl("https://cdn.hinchmart.com/products/rfq_material_default.jpg")
                .quantity(rfq.getQuantity())
                .unit(rfq.getUnit())
                .unitPrice(quote.getUnitPrice())
                .originalPrice(quote.getUnitPrice())
                .appliedTier("RFQ Accepted Bid Rate")
                .gstRate(BigDecimal.valueOf(18.0))
                .lineTotal(taxable)
                .lineGst(gst)
                .build();
        orderItemRepository.save(orderItem);

        TrackingCheckpoint checkpoint = TrackingCheckpoint.builder()
                .order(savedOrder)
                .status("ORDER_PLACED")
                .title("RFQ Accepted & Order Created")
                .location("HinchMart Central Enterprise Procurement Desk")
                .description("RFQ #" + rfq.getRfqNumber() + " accepted with vendor " + quote.getVendorName())
                .timestamp(LocalDateTime.now())
                .build();
        savedOrder.getCheckpoints().add(checkpoint);
        orderRepository.save(savedOrder);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quoteId", quote.getQuoteId());
        result.put("rfqId", rfq.getRfqId());
        result.put("orderId", savedOrder.getOrderId());
        result.put("orderNumber", savedOrder.getOrderNumber());
        result.put("totalAmount", savedOrder.getTotalAmount());
        result.put("status", "CONVERTED_TO_ORDER");

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RfqQuestionResponse> getRfqQuestions(Integer rfqId) {
        return rfqQuestionRepository.findByRfq_RfqIdOrderByCreatedAtAsc(rfqId).stream()
                .map(this::mapToQuestionResponse).toList();
    }

    @Override
    public RfqQuestionResponse addRfqQuestion(Integer rfqId, RfqQuestionRequest request) {
        Rfq rfq = findRfq(rfqId);

        RfqQuestion question = RfqQuestion.builder()
                .rfq(rfq)
                .question(request.getQuestion())
                .status("PENDING")
                .build();

        return mapToQuestionResponse(rfqQuestionRepository.save(question));
    }

    @Override
    public RfqQuestionResponse answerRfqQuestion(Integer questionId, String responseText) {
        RfqQuestion question = rfqQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        question.setResponse(responseText);
        question.setStatus("ANSWERED");
        question.setAnsweredAt(LocalDateTime.now());

        return mapToQuestionResponse(rfqQuestionRepository.save(question));
    }

    private Rfq findRfq(Integer id) {
        return rfqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found with id: " + id));
    }

    private RfqResponse mapToRfqResponse(Rfq r) {
        return RfqResponse.builder()
                .rfqId(r.getRfqId())
                .rfqNumber(r.getRfqNumber())
                .title(r.getTitle())
                .category(r.getCategory())
                .productMaterial(r.getProductMaterial())
                .quantity(r.getQuantity())
                .unit(r.getUnit())
                .technicalGrade(r.getTechnicalGrade())
                .mtcRequired(r.isMtcRequired())
                .deliveryLocation(r.getDeliveryLocation())
                .requiredByDate(r.getRequiredByDate())
                .siteAccess(r.getSiteAccess())
                .craneRequired(r.isCraneRequired())
                .targetBudget(r.getTargetBudget())
                .paymentTerms(r.getPaymentTerms())
                .specifications(r.getSpecifications())
                .boqAttachmentUrl(r.getBoqAttachmentUrl())
                .status(r.getStatus())
                .quotesCount(r.getQuotesCount() != null ? r.getQuotesCount() : 0)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private QuotationResponse mapToQuotationResponse(Quotation q) {
        return QuotationResponse.builder()
                .quoteId(q.getQuoteId())
                .rfqId(q.getRfq().getRfqId())
                .vendorId(q.getVendorId())
                .vendorName(q.getVendorName())
                .unitPrice(q.getUnitPrice())
                .totalAmount(q.getTotalAmount())
                .deliveryLeadTimeDays(q.getDeliveryLeadTimeDays())
                .paymentTermsOffered(q.getPaymentTermsOffered())
                .mtcIncluded(q.isMtcIncluded())
                .freightIncluded(q.isFreightIncluded())
                .validUntil(q.getValidUntil())
                .vendorRating(q.getVendorRating())
                .status(q.getStatus())
                .build();
    }

    private RfqQuestionResponse mapToQuestionResponse(RfqQuestion q) {
        return RfqQuestionResponse.builder()
                .questionId(q.getQuestionId())
                .rfqId(q.getRfq().getRfqId())
                .question(q.getQuestion())
                .response(q.getResponse())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .answeredAt(q.getAnsweredAt())
                .build();
    }
}
