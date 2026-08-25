package com.example.project.customer.service;

import com.example.project.customer.common.PagedResponse;
import com.example.project.customer.dto.*;
import com.example.project.customer.entity.*;
import com.example.project.customer.exception.BadRequestException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class RfqServiceImpl implements RfqService {

    private final RfqRepository rfqRepository;
    private final RfqQuotationRepository quotationRepository;
    private final RfqQuestionRepository questionRepository;
    private final OrderRepository orderRepository;
    private final UserProfileRepository userProfileRepository;

    public RfqServiceImpl(RfqRepository rfqRepository,
                          RfqQuotationRepository quotationRepository,
                          RfqQuestionRepository questionRepository,
                          OrderRepository orderRepository,
                          UserProfileRepository userProfileRepository) {
        this.rfqRepository = rfqRepository;
        this.quotationRepository = quotationRepository;
        this.questionRepository = questionRepository;
        this.orderRepository = orderRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public RfqResponse createRfq(Integer userId, RfqRequest request) {
        Integer uid = userId != null ? userId : 101;

        Rfq rfq = new Rfq();
        rfq.setUserId(uid);
        rfq.setTitle(request.getTitle());
        rfq.setCategory(request.getCategory());
        rfq.setProductMaterial(request.getProductMaterial());
        rfq.setQuantity(request.getQuantity());
        rfq.setUnit(request.getUnit());
        rfq.setTechnicalGrade(request.getTechnicalGrade());
        rfq.setMtcRequired(request.isMtcRequired());
        rfq.setDeliveryLocation(request.getDeliveryLocation());
        rfq.setRequiredByDate(request.getRequiredByDate());
        rfq.setSiteAccess(request.getSiteAccess());
        rfq.setCraneRequired(request.isCraneRequired());
        rfq.setTargetBudget(request.getTargetBudget());
        rfq.setPaymentTerms(request.getPaymentTerms());
        rfq.setSpecifications(request.getSpecifications());
        rfq.setBoqAttachmentUrl(request.getBoqAttachmentUrl());
        rfq.setStatus("OPEN");

        long tempNumber = System.currentTimeMillis() % 100000;
        rfq.setRfqNumber("RFQ-" + LocalDate.now().getYear() + "-" + String.format("%06d", tempNumber));

        Rfq saved = rfqRepository.save(rfq);
        saved.setRfqNumber("RFQ-" + LocalDate.now().getYear() + "-" + String.format("%06d", saved.getRfqId()));
        rfqRepository.save(saved);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RfqResponse> getRfqs(Integer userId, String status, int page, int limit) {
        Integer uid = userId != null ? userId : 101;
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 20;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rfq> paged;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            paged = rfqRepository.findByUserIdAndStatusIgnoreCase(uid, status.trim(), pageable);
        } else {
            paged = rfqRepository.findByUserId(uid, pageable);
        }

        List<RfqResponse> dtos = paged.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(dtos, page, pageSize, paged.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public RfqResponse getRfqById(Integer userId, Integer rfqId) {
        Integer uid = userId != null ? userId : 101;
        Rfq rfq = rfqRepository.findByRfqIdAndUserId(rfqId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found with id: " + rfqId));
        return toResponse(rfq);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RfqQuotationDto> getQuotations(Integer userId, Integer rfqId) {
        Integer uid = userId != null ? userId : 101;
        Rfq rfq = rfqRepository.findByRfqIdAndUserId(rfqId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found with id: " + rfqId));

        List<RfqQuotation> quotes = quotationRepository.findByRfq_RfqId(rfq.getRfqId());
        return quotes.stream().map(q -> new RfqQuotationDto(
                q.getQuoteId(),
                rfq.getRfqId(),
                q.getVendorId(),
                q.getVendorName(),
                q.getUnitPrice(),
                q.getTotalAmount(),
                q.getDeliveryLeadTimeDays(),
                q.getPaymentTermsOffered(),
                q.isMtcIncluded(),
                q.isFreightIncluded(),
                q.getValidUntil(),
                q.getVendorRating(),
                q.getStatus()
        )).toList();
    }

    @Override
    public AcceptQuotationResponse acceptQuotation(Integer userId, Integer quoteId) {
        Integer uid = userId != null ? userId : 101;
        RfqQuotation quotation = quotationRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + quoteId));

        Rfq rfq = quotation.getRfq();
        if (rfq == null) {
            throw new BadRequestException("Quotation is not attached to a valid RFQ");
        }

        quotation.setStatus("ACCEPTED");
        rfq.setStatus("CONVERTED_TO_ORDER");
        quotationRepository.save(quotation);
        rfqRepository.save(rfq);

        // Convert directly into confirmed purchase order
        Order order = new Order();
        order.setUserId(uid);
        order.setSubtotal(quotation.getTotalAmount());
        order.setDiscount(BigDecimal.ZERO);
        order.setTaxableAmount(quotation.getTotalAmount());

        BigDecimal totalGst = quotation.getTotalAmount().multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        order.setTotalGst(totalGst);
        order.setCgst(totalGst.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP));
        order.setSgst(totalGst.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP));
        order.setIgst(BigDecimal.ZERO);
        order.setFreightCharge(BigDecimal.ZERO);
        order.setCraneUnloadingCharge(rfq.isCraneRequired() ? new BigDecimal("2500.00") : BigDecimal.ZERO);
        order.setTotalAmount(quotation.getTotalAmount().add(totalGst).add(order.getCraneUnloadingCharge()));
        order.setPaymentMethod(rfq.getPaymentTerms() != null ? rfq.getPaymentTerms() : "LETTER_OF_CREDIT");
        order.setPaymentStatus("PENDING");
        order.setOrderStatus("CONFIRMED");
        order.setDeliverySlot("AS_PER_RFQ_SCHEDULE");
        order.setDeliveryInstructions("RFQ Order: " + rfq.getTitle() + ". Delivery at: " + rfq.getDeliveryLocation());
        order.setPoNumber("PO-RFQ-" + rfq.getRfqId());
        order.setRequiresCraneUnloading(rfq.isCraneRequired());
        order.setEstimatedDelivery(LocalDateTime.now().plusDays(quotation.getDeliveryLeadTimeDays()));
        order.setCarrierName("JSW Logistics Fleet");
        order.setVehicleNumber("TS 08 TC 9918 (Heavy Consignment Trailer)");
        order.setDriverName("Suresh Babu (+91 9849556677)");

        UserProfile profile = userProfileRepository.findById(uid).orElse(null);
        if (profile != null) {
            order.setBuyerLegalName(profile.getCompanyName() != null ? profile.getCompanyName() : profile.getFullName());
            order.setBuyerGstin(profile.getGstNumber() != null ? profile.getGstNumber() : "36AAACT2727Q1ZW");
        }

        String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        long tempId = System.currentTimeMillis() % 10000;
        order.setOrderNumber("ORD-" + dateStr + "-" + tempId);

        Order savedOrder = orderRepository.save(order);
        savedOrder.setOrderNumber("ORD-" + dateStr + "-" + savedOrder.getOrderId());
        savedOrder.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + String.format("%06d", savedOrder.getOrderId()));
        savedOrder.setPdfUrl("https://cdn.hinchmart.com/invoices/" + savedOrder.getInvoiceNumber() + ".pdf");
        savedOrder.setTrackingNumber("VRL-HYD-" + LocalDate.now().getYear() + "-" + savedOrder.getOrderId());

        OrderItem oi = new OrderItem(
                null,
                rfq.getProductMaterial() + " (" + rfq.getTechnicalGrade() + ")",
                "https://cdn.hinchmart.com/products/tmt_steel_12mm.jpg",
                rfq.getQuantity(),
                rfq.getUnit(),
                quotation.getUnitPrice(),
                quotation.getTotalAmount(),
                18.0,
                totalGst
        );
        savedOrder.addItem(oi);

        OrderTrackingCheckpoint cp = new OrderTrackingCheckpoint(
                "ORDER_PLACED",
                "RFQ Quotation Converted to Order",
                "HinchMart Enterprise Procurement Hub",
                LocalDateTime.now(),
                "Vendor: " + quotation.getVendorName() + " bid accepted.",
                1
        );
        savedOrder.addCheckpoint(cp);

        orderRepository.save(savedOrder);

        return new AcceptQuotationResponse(
                quotation.getQuoteId(),
                rfq.getRfqId(),
                savedOrder.getOrderId(),
                savedOrder.getOrderNumber(),
                quotation.getTotalAmount(),
                "CONVERTED_TO_ORDER"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RfqQuestionDto> getQuestions(Integer userId, Integer rfqId) {
        List<RfqQuestion> list = questionRepository.findByRfq_RfqIdOrderByCreatedAtAsc(rfqId);
        return list.stream().map(q -> new RfqQuestionDto(
                q.getQuestionId(),
                rfqId,
                q.getQuestion(),
                q.getResponse(),
                q.getStatus(),
                q.getCreatedAt()
        )).toList();
    }

    @Override
    public RfqQuestionDto addQuestion(Integer userId, Integer rfqId, RfqQuestionRequest request) {
        Integer uid = userId != null ? userId : 101;
        Rfq rfq = rfqRepository.findByRfqIdAndUserId(rfqId, uid)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found with id: " + rfqId));

        RfqQuestion q = new RfqQuestion(request.getQuestion(), null, "PENDING");
        q.setRfq(rfq);
        RfqQuestion saved = questionRepository.save(q);

        return new RfqQuestionDto(
                saved.getQuestionId(),
                rfqId,
                saved.getQuestion(),
                saved.getResponse(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    private RfqResponse toResponse(Rfq r) {
        RfqResponse resp = new RfqResponse();
        resp.setRfqId(r.getRfqId());
        resp.setRfqNumber(r.getRfqNumber());
        resp.setTitle(r.getTitle());
        resp.setCategory(r.getCategory());
        resp.setProductMaterial(r.getProductMaterial());
        resp.setQuantity(r.getQuantity());
        resp.setUnit(r.getUnit());
        resp.setTechnicalGrade(r.getTechnicalGrade());
        resp.setMtcRequired(r.isMtcRequired());
        resp.setDeliveryLocation(r.getDeliveryLocation());
        resp.setRequiredByDate(r.getRequiredByDate());
        resp.setSiteAccess(r.getSiteAccess());
        resp.setCraneRequired(r.isCraneRequired());
        resp.setTargetBudget(r.getTargetBudget());
        resp.setPaymentTerms(r.getPaymentTerms());
        resp.setSpecifications(r.getSpecifications());
        resp.setBoqAttachmentUrl(r.getBoqAttachmentUrl());
        resp.setStatus(r.getStatus());
        resp.setQuotesCount(r.getQuotesCount());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
