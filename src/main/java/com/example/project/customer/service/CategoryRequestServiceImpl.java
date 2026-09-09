package com.example.project.customer.service;

import com.example.project.customer.dto.ApiResponse;
import com.example.project.customer.dto.CategoryRequestCreateRequest;
import com.example.project.customer.dto.CategoryRequestResponse;
import com.example.project.customer.dto.PaginationMeta;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CategoryRequest;
import com.example.project.customer.entity.CategoryRequestStatus;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CategoryRequestRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CategoryRequestServiceImpl implements CategoryRequestService {

    private final CategoryRequestRepository categoryRequestRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Override
    @Transactional
    public CategoryRequestResponse submitCategoryRequest(Integer sellerId, CategoryRequestCreateRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        CategoryRequest catReq = CategoryRequest.builder()
                .seller(seller)
                .name(request.getName().trim())
                .parentCategoryId(request.getParentCategoryId())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(CategoryRequestStatus.PENDING)
                .build();

        CategoryRequest saved = categoryRequestRepository.save(catReq);
        log.info("Seller #{} submitted category request #{} ('{}')", sellerId, saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryRequestResponse> getSellerRequests(Integer sellerId) {
        return categoryRequestRepository.findBySeller_SellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CategoryRequestResponse>> getAdminRequests(CategoryRequestStatus status, int page, int limit) {
        int pageNumber = page > 0 ? page : 1;
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<CategoryRequest> pageResult = (status != null)
                ? categoryRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : categoryRequestRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<CategoryRequestResponse> list = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(pageNumber, pageSize, pageResult.getTotalElements());
        return ApiResponse.paginated("Category requests retrieved successfully", list, meta);
    }

    @Override
    @Transactional
    public CategoryRequestResponse approveCategoryRequest(Integer requestId) {
        CategoryRequest catReq = categoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Category request not found with id: " + requestId));

        if (catReq.getStatus() != CategoryRequestStatus.APPROVED) {
            String baseSlug = slugify(catReq.getName());
            if (catReq.getParentCategoryId() == null) {
                // Top-level category insertion
                String slug = baseSlug;
                int count = 1;
                while (categoryRepository.findBySlugIgnoreCase(slug).isPresent()) {
                    slug = baseSlug + "-" + count++;
                }
                Category cat = Category.builder()
                        .name(catReq.getName())
                        .slug(slug)
                        .active(true)
                        .build();
                categoryRepository.save(cat);
                log.info("Approved Category Request #{}: Created top-level Category '{}' (slug: {})", requestId, cat.getName(), slug);
            } else {
                // Subcategory insertion
                Category parent = categoryRepository.findById(catReq.getParentCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + catReq.getParentCategoryId()));

                String slug = baseSlug;
                int count = 1;
                while (subcategoryRepository.findBySlugIgnoreCase(slug).isPresent()) {
                    slug = baseSlug + "-" + count++;
                }
                Subcategory sub = Subcategory.builder()
                        .category(parent)
                        .name(catReq.getName())
                        .slug(slug)
                        .active(true)
                        .build();
                subcategoryRepository.save(sub);
                log.info("Approved Category Request #{}: Created Subcategory '{}' under Category '{}'", requestId, sub.getName(), parent.getName());
            }

            catReq.setStatus(CategoryRequestStatus.APPROVED);
            catReq.setRejectionReason(null);
            categoryRequestRepository.save(catReq);
        }

        return mapToResponse(catReq);
    }

    @Override
    @Transactional
    public CategoryRequestResponse rejectCategoryRequest(Integer requestId, String reason) {
        CategoryRequest catReq = categoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Category request not found with id: " + requestId));

        catReq.setStatus(CategoryRequestStatus.REJECTED);
        catReq.setRejectionReason(reason != null ? reason.trim() : "Proposed category does not align with marketplace taxonomy.");
        CategoryRequest saved = categoryRequestRepository.save(catReq);
        log.info("Rejected Category Request #{} (Reason: {})", requestId, saved.getRejectionReason());
        return mapToResponse(saved);
    }

    private CategoryRequestResponse mapToResponse(CategoryRequest r) {
        Seller seller = r.getSeller();
        String sellerName = seller != null ? (seller.getCompanyName() != null ? seller.getCompanyName() : seller.getName()) : null;
        return CategoryRequestResponse.builder()
                .id(r.getId())
                .sellerId(seller != null ? seller.getSellerId() : null)
                .sellerName(sellerName)
                .name(r.getName())
                .parentCategoryId(r.getParentCategoryId())
                .description(r.getDescription())
                .status(r.getStatus())
                .rejectionReason(r.getRejectionReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String slugify(String input) {
        if (input == null) return "category";
        String nonLatin = "[^\\w\\s-]";
        String whitespace = "[\\s+]";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized.replaceAll(nonLatin, "").replaceAll(whitespace, "-").toLowerCase();
        return slug.replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
