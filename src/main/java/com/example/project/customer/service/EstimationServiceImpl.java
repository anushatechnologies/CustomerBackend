package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.dto.estimation.CandidateProductSummary;
import com.example.project.customer.dto.estimation.EstimationItemResponse;
import com.example.project.customer.dto.estimation.EstimationResponse;
import com.example.project.customer.dto.estimation.ExtractedRequirementItem;
import com.example.project.customer.dto.estimation.ExtractedRequirementList;
import com.example.project.customer.entity.Estimation;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.EstimationStatus;
import com.example.project.customer.entity.MatchStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.EstimationNotFoundException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.exception.UnauthorizedAccessException;
import com.example.project.customer.repository.EstimationItemRepository;
import com.example.project.customer.repository.EstimationRepository;
import com.example.project.customer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EstimationServiceImpl implements EstimationService {

    private final DocumentProcessingService documentProcessingService;
    private final AiRequirementService aiRequirementService;
    private final RequirementMatchingService requirementMatchingService;
    private final EstimationCalculationService estimationCalculationService;
    private final QuotationPdfService quotationPdfService;
    private final S3ImageService s3ImageService;
    private final EstimationRepository estimationRepository;
    private final EstimationItemRepository estimationItemRepository;
    private final ProductRepository productRepository;

    @Override
    public EstimationResponse uploadAndProcess(Integer customerId, MultipartFile file) {
        int uid = customerId != null ? customerId : 101;
        log.info("Starting requirement estimation workflow for customerId={}, fileName={}", uid, file.getOriginalFilename());

        // 1. Validate file (type, size <= 15MB, non-empty, corrupted check)
        documentProcessingService.validateFile(file);

        // 2. Create initial estimation record to generate ID
        String estimationNumber = generateEstimationNumber();
        Estimation estimation = Estimation.builder()
                .estimationNumber(estimationNumber)
                .customerId(uid)
                .originalFileName(file.getOriginalFilename())
                .originalFileType(file.getContentType())
                .originalFileSize(file.getSize())
                .status(EstimationStatus.PROCESSING)
                .build();

        estimation = estimationRepository.save(estimation);

        // 3. Store original requirement file in AWS S3 at: estimations/{customerId}/{estimationId}/requirement.{ext}
        String extension = getFileExtension(file.getOriginalFilename());
        String originalS3Key = "estimations/" + uid + "/" + estimation.getId() + "/requirement" + extension;
        ImageUploadResponse uploadResponse = s3ImageService.uploadFileToKey(file, originalS3Key);

        estimation.setOriginalFileKey(uploadResponse.getImageKey());
        estimation.setOriginalFileUrl(uploadResponse.getFileUrl());
        estimation = estimationRepository.save(estimation);

        try {
            // 4. Process document (PDF text extraction vs Vision bytes)
            String extractedText = "";
            byte[] visionImageBytes = null;

            if (documentProcessingService.isPdf(file)) {
                extractedText = documentProcessingService.extractTextFromPdf(file);
                if (extractedText.isBlank()) {
                    // PDF contains scanned images - render first page for Ollama vision
                    visionImageBytes = documentProcessingService.renderFirstPageToImage(file);
                }
            } else {
                // Image format (JPG, JPEG, PNG)
                visionImageBytes = documentProcessingService.getFileBytes(file);
            }

            // 5. Extract structured requirements via Spring AI Ollama
            ExtractedRequirementList requirements = aiRequirementService.extractRequirements(file, extractedText, visionImageBytes);

            // 6. Match requirements against existing HinchMart Product Catalog
            boolean hasAmbiguousMatches = false;
            for (ExtractedRequirementItem req : requirements.getItems()) {
                EstimationItem item = EstimationItem.builder()
                        .estimation(estimation)
                        .rawItemName(req.getName())
                        .requestedQuantity(req.getQuantity())
                        .requestedUnit(req.getUnit())
                        .requestedBrand(req.getBrand())
                        .dimensions(req.getDimensions())
                        .specifications(req.getSpecification())
                        .notes(req.getNotes())
                        .build();

                requirementMatchingService.matchRequirementItem(item, req);
                estimationCalculationService.calculateItemPricing(item);

                if (item.getMatchStatus() == MatchStatus.MULTIPLE_MATCHES) {
                    hasAmbiguousMatches = true;
                }

                estimation.addItem(item);
            }

            // 7. Calculate overall estimation totals (subtotal, tax, bulk discount)
            estimationCalculationService.recalculateEstimationTotals(estimation);

            if (requirements.getProjectSummary() != null) {
                estimation.setNotes(requirements.getProjectSummary());
            }

            estimation.setStatus(hasAmbiguousMatches ? EstimationStatus.REQUIREMENTS_EXTRACTED : EstimationStatus.RESOLVED);
            estimation = estimationRepository.save(estimation);

            log.info("Estimation successfully created and calculated: id={}, number={}, items={}, status={}",
                    estimation.getId(), estimation.getEstimationNumber(), estimation.getItems().size(), estimation.getStatus());

            return toResponse(estimation);
        } catch (Exception e) {
            log.error("Failed processing estimation id={}: {}", estimation.getId(), e.getMessage(), e);
            estimation.setStatus(EstimationStatus.FAILED);
            estimation.setFailureReason(e.getMessage());
            estimationRepository.save(estimation);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EstimationResponse getEstimation(Integer customerId, Long estimationId) {
        Estimation estimation = findAndValidateOwnership(estimationId, customerId);
        return toResponse(estimation);
    }

    @Override
    public EstimationResponse resolveItemMatch(Integer customerId, Long estimationId, Long itemId, Integer productId) {
        Estimation estimation = findAndValidateOwnership(estimationId, customerId);

        EstimationItem item = estimationItemRepository.findByIdAndEstimation_Id(itemId, estimationId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimation item not found with id: " + itemId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        log.info("Resolving ambiguous item '{}' with selected product '{}' (id={})",
                item.getRawItemName(), product.getTitle(), productId);

        requirementMatchingService.applySelectedProduct(item, product);
        item.setMatchStatus(MatchStatus.RESOLVED);
        estimationCalculationService.calculateItemPricing(item);
        estimationItemRepository.save(item);

        // Recalculate totals
        estimationCalculationService.recalculateEstimationTotals(estimation);

        boolean hasRemainingAmbiguous = estimation.getItems().stream()
                .anyMatch(i -> i.getMatchStatus() == MatchStatus.MULTIPLE_MATCHES);

        if (!hasRemainingAmbiguous && estimation.getStatus() == EstimationStatus.REQUIREMENTS_EXTRACTED) {
            estimation.setStatus(EstimationStatus.RESOLVED);
        }

        estimation = estimationRepository.save(estimation);
        return toResponse(estimation);
    }

    @Override
    public EstimationResponse generateQuotation(Integer customerId, Long estimationId) {
        Estimation estimation = findAndValidateOwnership(estimationId, customerId);

        log.info("Generating Quotation PDF for estimation: id={}, number={}", estimationId, estimation.getEstimationNumber());

        // Refresh calculations with current database pricing
        estimationCalculationService.recalculateEstimationTotals(estimation);

        // Generate and upload PDF to AWS S3 at: estimations/{customerId}/{estimationId}/quotation.pdf
        String pdfUrl = quotationPdfService.generateAndUploadQuotationPdf(estimation);

        estimation.setStatus(EstimationStatus.QUOTATION_GENERATED);
        estimation = estimationRepository.save(estimation);

        log.info("Quotation PDF generated and uploaded to S3: {}", pdfUrl);
        return toResponse(estimation);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadQuotationPdf(Integer customerId, Long estimationId) {
        Estimation estimation = findAndValidateOwnership(estimationId, customerId);

        if (estimation.getQuotationPdfKey() != null && !estimation.getQuotationPdfKey().isBlank()) {
            try {
                return s3ImageService.downloadImage(estimation.getQuotationPdfKey());
            } catch (Exception e) {
                log.warn("Could not download cached quotation from S3 key '{}': {}. Re-generating PDF directly.",
                        estimation.getQuotationPdfKey(), e.getMessage());
            }
        }

        return quotationPdfService.generateQuotationPdf(estimation);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<EstimationResponse>> getCustomerEstimations(Integer customerId, int page, int limit) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.max(1, limit);
        PageRequest pageRequest = PageRequest.of(safePage - 1, safeLimit);

        int uid = customerId != null ? customerId : 101;
        Page<Estimation> pageResult = estimationRepository.findByCustomerIdOrderByCreatedAtDesc(uid, pageRequest);

        List<EstimationResponse> responses = pageResult.getContent().stream()
                .map(this::toResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(safePage, safeLimit, pageResult.getTotalElements());
        return ApiResponse.paginated("Estimations retrieved successfully", responses, meta);
    }

    private Estimation findAndValidateOwnership(Long estimationId, Integer customerId) {
        Estimation estimation = estimationRepository.findById(estimationId)
                .orElseThrow(() -> new EstimationNotFoundException("Estimation not found with id: " + estimationId));

        int uid = customerId != null ? customerId : 101;
        if (!estimation.getCustomerId().equals(uid)) {
            log.warn("Security violation: customer {} attempted to access estimation {} owned by customer {}",
                    uid, estimationId, estimation.getCustomerId());
            throw new UnauthorizedAccessException("You are not authorized to view or modify this estimation");
        }

        return estimation;
    }

    private String generateEstimationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "EST-" + datePart + "-" + randomSuffix;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".pdf";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private EstimationResponse toResponse(Estimation e) {
        int totalItems = 0;
        int matchedItems = 0;
        int ambiguousItems = 0;
        int unavailableItems = 0;

        List<EstimationItemResponse> itemResponses = new ArrayList<>();
        if (e.getItems() != null) {
            totalItems = e.getItems().size();
            for (EstimationItem item : e.getItems()) {
                if (item.getMatchStatus() == MatchStatus.MATCHED || item.getMatchStatus() == MatchStatus.RESOLVED) {
                    matchedItems++;
                } else if (item.getMatchStatus() == MatchStatus.MULTIPLE_MATCHES) {
                    ambiguousItems++;
                } else if (item.getMatchStatus() == MatchStatus.NOT_FOUND) {
                    unavailableItems++;
                }

                List<CandidateProductSummary> candidateSummaries = new ArrayList<>();
                if (item.getMatchStatus() == MatchStatus.MULTIPLE_MATCHES) {
                    candidateSummaries = requirementMatchingService.getCandidateSummaries(item.getCandidateProductIds());
                }

                Product p = item.getMatchedProduct();
                EstimationItemResponse ir = EstimationItemResponse.builder()
                        .itemId(item.getId())
                        .rawItemName(item.getRawItemName())
                        .requestedQuantity(item.getRequestedQuantity())
                        .requestedUnit(item.getRequestedUnit())
                        .requestedBrand(item.getRequestedBrand())
                        .dimensions(item.getDimensions())
                        .specifications(item.getSpecifications())
                        .notes(item.getNotes())
                        .matchStatus(item.getMatchStatus())
                        .matchedProductId(p != null ? p.getProductId() : null)
                        .matchedProductTitle(p != null ? p.getTitle() : null)
                        .matchedProductSku(p != null ? p.getSku() : null)
                        .matchedProductImageUrl(p != null ? p.getImageUrl() : null)
                        .matchedProductUnit(p != null ? p.getUnit() : null)
                        .unitPrice(item.getUnitPrice())
                        .appliedTierDescription(item.getAppliedTierDescription())
                        .gstRate(item.getGstRate())
                        .lineSubtotal(item.getLineSubtotal())
                        .lineTax(item.getLineTax())
                        .lineTotal(item.getLineTotal())
                        .isAvailable(item.getIsAvailable())
                        .availableStock(item.getAvailableStock())
                        .candidateProducts(candidateSummaries)
                        .build();

                itemResponses.add(ir);
            }
        }

        return EstimationResponse.builder()
                .estimationId(e.getId())
                .estimationNumber(e.getEstimationNumber())
                .customerId(e.getCustomerId())
                .originalFileName(e.getOriginalFileName())
                .originalFileType(e.getOriginalFileType())
                .originalFileSize(e.getOriginalFileSize())
                .originalFileUrl(e.getOriginalFileUrl())
                .status(e.getStatus())
                .failureReason(e.getFailureReason())
                .quotationPdfUrl(e.getQuotationPdfUrl())
                .subtotal(e.getSubtotal())
                .taxAmount(e.getTaxAmount())
                .discountAmount(e.getDiscountAmount())
                .grandTotal(e.getGrandTotal())
                .totalItemsCount(totalItems)
                .matchedItemsCount(matchedItems)
                .ambiguousItemsCount(ambiguousItems)
                .unavailableItemsCount(unavailableItems)
                .notes(e.getNotes())
                .items(itemResponses)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
