package com.example.project.customer.service;

import com.example.project.customer.dto.settlement.CommissionRuleCreateRequest;
import com.example.project.customer.dto.settlement.CommissionRuleResponse;
import com.example.project.customer.dto.settlement.CommissionRuleUpdateRequest;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CommissionRule;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionRuleServiceImpl implements CommissionRuleService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRuleResponse> getRules(Integer categoryId, Boolean activeOnly) {
        List<CommissionRule> rules;
        if (categoryId != null) {
            rules = commissionRuleRepository.findByCategory_CategoryIdOrderByCreatedAtDesc(categoryId);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            rules = commissionRuleRepository.findByIsActiveOrderByCreatedAtDesc(true);
        } else {
            rules = commissionRuleRepository.findAllByOrderByCreatedAtDesc();
        }

        if (Boolean.TRUE.equals(activeOnly) && categoryId != null) {
            rules = rules.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).toList();
        }

        return rules.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionRuleResponse getRuleById(Integer ruleId) {
        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission rule not found with id: " + ruleId));
        return mapToResponse(rule);
    }

    @Override
    @Transactional
    public CommissionRuleResponse createRule(CommissionRuleCreateRequest request) {
        validateRate(request.getRate());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        }

        LocalDateTime effectiveFrom = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom()
                : LocalDateTime.now();

        boolean isActive = request.getIsActive() != null ? request.getIsActive() : true;

        // If activating a global rule, ensure audit-safe transition by deactivating older active global rules
        if (category == null && isActive) {
            deactivatePreviousActiveGlobalRules(effectiveFrom);
        }

        CommissionRule rule = CommissionRule.builder()
                .category(category)
                .rate(request.getRate().setScale(2, RoundingMode.HALF_UP))
                .isActive(isActive)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(request.getEffectiveTo())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CommissionRule saved = commissionRuleRepository.save(rule);
        log.info("Admin created commission rule #{} for categoryId={}, rate={}%, isActive={}",
                saved.getRuleId(), category != null ? category.getCategoryId() : "GLOBAL", saved.getRate(), saved.getIsActive());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CommissionRuleResponse updateRule(Integer ruleId, CommissionRuleUpdateRequest request) {
        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission rule not found with id: " + ruleId));

        validateRate(request.getRate());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
        }

        boolean willBeActive = request.getIsActive() != null ? request.getIsActive() : rule.getIsActive();
        LocalDateTime effectiveFrom = request.getEffectiveFrom() != null ? request.getEffectiveFrom() : rule.getEffectiveFrom();

        // If activating this rule as a global rule, safely deactivate other active global rules
        if (category == null && willBeActive && !Boolean.TRUE.equals(rule.getIsActive())) {
            deactivatePreviousActiveGlobalRules(effectiveFrom, ruleId);
        }

        rule.setCategory(category);
        rule.setRate(request.getRate().setScale(2, RoundingMode.HALF_UP));
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }
        if (request.getEffectiveFrom() != null) {
            rule.setEffectiveFrom(request.getEffectiveFrom());
        }
        rule.setEffectiveTo(request.getEffectiveTo());
        rule.setUpdatedAt(LocalDateTime.now());

        CommissionRule updated = commissionRuleRepository.save(rule);
        log.info("Admin updated commission rule #{}: categoryId={}, rate={}%, isActive={}",
                updated.getRuleId(), category != null ? category.getCategoryId() : "GLOBAL", updated.getRate(), updated.getIsActive());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CommissionRuleResponse toggleRuleStatus(Integer ruleId, Boolean isActive) {
        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission rule not found with id: " + ruleId));

        boolean targetStatus = isActive != null ? isActive : !Boolean.TRUE.equals(rule.getIsActive());

        if (targetStatus && rule.getCategory() == null) {
            deactivatePreviousActiveGlobalRules(LocalDateTime.now(), ruleId);
        }

        rule.setIsActive(targetStatus);
        rule.setUpdatedAt(LocalDateTime.now());
        CommissionRule saved = commissionRuleRepository.save(rule);
        log.info("Admin toggled commission rule #{} isActive to {}", ruleId, targetStatus);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRule(Integer ruleId) {
        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission rule not found with id: " + ruleId));

        // Soft-deactivate by default to preserve financial audit trail
        rule.setIsActive(false);
        rule.setEffectiveTo(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        commissionRuleRepository.save(rule);
        log.info("Admin deactivated commission rule #{}", ruleId);
    }

    private void deactivatePreviousActiveGlobalRules(LocalDateTime effectiveDate, Integer excludeRuleId) {
        List<CommissionRule> activeGlobals = commissionRuleRepository.findActiveGlobalRules(effectiveDate);
        for (CommissionRule oldRule : activeGlobals) {
            if (excludeRuleId != null && oldRule.getRuleId().equals(excludeRuleId)) {
                continue;
            }
            log.info("Deactivating previous active global commission rule #{} in favor of newly activated rule", oldRule.getRuleId());
            oldRule.setIsActive(false);
            if (oldRule.getEffectiveTo() == null || oldRule.getEffectiveTo().isAfter(effectiveDate)) {
                oldRule.setEffectiveTo(effectiveDate);
            }
            oldRule.setUpdatedAt(LocalDateTime.now());
            commissionRuleRepository.save(oldRule);
        }
    }

    private void deactivatePreviousActiveGlobalRules(LocalDateTime effectiveDate) {
        deactivatePreviousActiveGlobalRules(effectiveDate, null);
    }

    private void validateRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Holding fee rate must be between 0.00 and 100.00 percent.");
        }
    }

    private CommissionRuleResponse mapToResponse(CommissionRule rule) {
        return CommissionRuleResponse.builder()
                .ruleId(rule.getRuleId())
                .categoryId(rule.getCategory() != null ? rule.getCategory().getCategoryId() : null)
                .categoryName(rule.getCategory() != null ? rule.getCategory().getName() : "Global Fallback")
                .rate(rule.getRate())
                .isActive(rule.getIsActive())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
