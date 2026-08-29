package com.example.project.customer.service;

import com.example.project.customer.dto.DiscountRejectionRequest;
import com.example.project.customer.dto.SellerDiscountRequest;
import com.example.project.customer.dto.SellerDiscountResponse;
import com.example.project.customer.entity.DiscountStatus;
import com.example.project.customer.entity.DiscountType;
import com.example.project.customer.entity.SellerDiscount;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.SellerDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SellerDiscountServiceImpl implements SellerDiscountService {

    private final SellerDiscountRepository repository;

    @Override
    public SellerDiscountResponse create(Integer sellerId, SellerDiscountRequest request) {
        validateRequest(request);

        if (repository.findByCodeIgnoreCase(request.code()).isPresent()) {
            throw new IllegalArgumentException("Discount code already exists: " + request.code());
        }

        SellerDiscount discount = SellerDiscount.builder()
                .sellerId(sellerId)
                .code(request.code())
                .description(request.description())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minimumOrderAmount(request.minimumOrderAmount())
                .maxDiscountAmount(request.maxDiscountAmount())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(DiscountStatus.PENDING)
                .active(request.active() != null ? request.active() : true)
                .build();

        return mapToResponse(repository.save(discount));
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDiscountResponse getById(Integer sellerId, Integer discountId) {
        SellerDiscount discount = repository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        if (!discount.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not allowed to access this discount");
        }

        return mapToResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDiscountResponse> getBySeller(Integer sellerId) {
        return repository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SellerDiscountResponse update(Integer sellerId, Integer discountId, SellerDiscountRequest request) {
        validateRequest(request);

        SellerDiscount discount = repository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        if (!discount.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not allowed to update this discount");
        }

        if (repository.existsByCodeIgnoreCaseAndSellerIdNot(request.code(), sellerId)) {
            throw new IllegalArgumentException("Discount code already exists: " + request.code());
        }

        discount.setCode(request.code());
        discount.setDescription(request.description());
        discount.setDiscountType(request.discountType());
        discount.setDiscountValue(request.discountValue());
        discount.setMinimumOrderAmount(request.minimumOrderAmount());
        discount.setMaxDiscountAmount(request.maxDiscountAmount());
        discount.setStartDate(request.startDate());
        discount.setEndDate(request.endDate());
        discount.setActive(request.active() != null ? request.active() : true);
        discount.setStatus(DiscountStatus.PENDING);
        discount.setRejectionReason(null);

        return mapToResponse(repository.save(discount));
    }

    @Override
    public SellerDiscountResponse submitForReview(Integer sellerId, Integer discountId) {
        SellerDiscount discount = repository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        if (!discount.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not allowed to submit this discount");
        }

        discount.setStatus(DiscountStatus.PENDING);
        discount.setActive(false);
        discount.setRejectionReason(null);

        return mapToResponse(repository.save(discount));
    }

    @Override
    public SellerDiscountResponse approve(Integer discountId) {
        SellerDiscount discount = repository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        discount.setStatus(DiscountStatus.APPROVED);
        discount.setActive(true);
        discount.setRejectionReason(null);

        return mapToResponse(repository.save(discount));
    }

    @Override
    public SellerDiscountResponse reject(Integer discountId, DiscountRejectionRequest request) {
        SellerDiscount discount = repository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        discount.setStatus(DiscountStatus.REJECTED);
        discount.setActive(false);
        discount.setRejectionReason(request.reason());

        return mapToResponse(repository.save(discount));
    }

    @Override
    public SellerDiscountResponse editByAdmin(Integer discountId, SellerDiscountRequest request) {
        validateRequest(request);

        SellerDiscount discount = repository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        if (repository.existsByCodeIgnoreCaseAndSellerIdNot(request.code(), discount.getSellerId())) {
            throw new IllegalArgumentException("Discount code already exists: " + request.code());
        }

        discount.setCode(request.code());
        discount.setDescription(request.description());
        discount.setDiscountType(request.discountType());
        discount.setDiscountValue(request.discountValue());
        discount.setMinimumOrderAmount(request.minimumOrderAmount());
        discount.setMaxDiscountAmount(request.maxDiscountAmount());
        discount.setStartDate(request.startDate());
        discount.setEndDate(request.endDate());
        discount.setActive(request.active() != null ? request.active() : true);
        discount.setAdminNote("Edited by admin during review");
        discount.setStatus(DiscountStatus.PENDING);
        discount.setRejectionReason(null);

        return mapToResponse(repository.save(discount));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDiscountResponse> getPendingForAdmin() {
        return repository.findByStatusOrderByCreatedAtAsc(DiscountStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDiscountResponse> getAllForAdmin() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDiscountResponse> getApplicableForCustomer() {
        LocalDate today = LocalDate.now();

        return repository.findByStatusAndActiveTrueOrderByCreatedAtDesc(DiscountStatus.APPROVED, true)
                .stream()
                .filter(d -> !today.isBefore(d.getStartDate()) && !today.isAfter(d.getEndDate()))
                .map(this::mapToResponse)
                .toList();
    }

    private void validateRequest(SellerDiscountRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (request.discountType() == DiscountType.PERCENTAGE && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }

        if (request.discountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero");
        }
    }

    private SellerDiscountResponse mapToResponse(SellerDiscount discount) {
        return SellerDiscountResponse.builder()
                .discountId(discount.getDiscountId())
                .sellerId(discount.getSellerId())
                .code(discount.getCode())
                .description(discount.getDescription())
                .discountType(discount.getDiscountType())
                .discountValue(discount.getDiscountValue())
                .minimumOrderAmount(discount.getMinimumOrderAmount())
                .maxDiscountAmount(discount.getMaxDiscountAmount())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .status(discount.getStatus())
                .active(discount.getActive())
                .rejectionReason(discount.getRejectionReason())
                .adminNote(discount.getAdminNote())
                .createdAt(discount.getCreatedAt())
                .updatedAt(discount.getUpdatedAt())
                .build();
    }
}
