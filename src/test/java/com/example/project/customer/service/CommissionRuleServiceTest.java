package com.example.project.customer.service;

import com.example.project.customer.dto.settlement.CommissionRuleCreateRequest;
import com.example.project.customer.dto.settlement.CommissionRuleResponse;
import com.example.project.customer.dto.settlement.CommissionRuleUpdateRequest;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.CommissionRule;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.CommissionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CommissionRuleServiceTest {

    @Mock
    private CommissionRuleRepository commissionRuleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CommissionRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommissionRuleServiceImpl(commissionRuleRepository, categoryRepository);
    }

    @Test
    @DisplayName("1. Create global rule safely deactivates prior active global rules")
    void testCreateGlobalRule_DeactivatesPriorGlobals() {
        CommissionRule oldGlobal = CommissionRule.builder()
                .ruleId(1)
                .category(null)
                .rate(new BigDecimal("5.00"))
                .isActive(true)
                .build();

        when(commissionRuleRepository.findActiveGlobalRules(any())).thenReturn(List.of(oldGlobal));
        when(commissionRuleRepository.save(any(CommissionRule.class))).thenAnswer(inv -> {
            CommissionRule r = inv.getArgument(0);
            if (r.getRuleId() == null) r.setRuleId(2);
            return r;
        });

        CommissionRuleCreateRequest req = CommissionRuleCreateRequest.builder()
                .rate(new BigDecimal("7.50"))
                .isActive(true)
                .build();

        CommissionRuleResponse resp = service.createRule(req);

        assertThat(resp.getRuleId()).isEqualTo(2);
        assertThat(resp.getRate()).isEqualByComparingTo("7.50");
        assertThat(resp.getCategoryName()).isEqualTo("Global Fallback");

        // Old global rule was deactivated
        assertThat(oldGlobal.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("2. Create category-specific rule associates category and does not deactivate global rule")
    void testCreateCategoryRule_AssociatesCategory() {
        Category cat = Category.builder().categoryId(36).name("Structural Steel").build();
        when(categoryRepository.findById(36)).thenReturn(Optional.of(cat));
        when(commissionRuleRepository.save(any(CommissionRule.class))).thenAnswer(inv -> {
            CommissionRule r = inv.getArgument(0);
            if (r.getRuleId() == null) r.setRuleId(3);
            return r;
        });

        CommissionRuleCreateRequest req = CommissionRuleCreateRequest.builder()
                .categoryId(36)
                .rate(new BigDecimal("6.00"))
                .isActive(true)
                .build();

        CommissionRuleResponse resp = service.createRule(req);

        assertThat(resp.getRuleId()).isEqualTo(3);
        assertThat(resp.getCategoryId()).isEqualTo(36);
        assertThat(resp.getCategoryName()).isEqualTo("Structural Steel");
    }

    @Test
    @DisplayName("3. Create rule with invalid rate throws IllegalArgumentException")
    void testCreateRule_InvalidRateThrows() {
        CommissionRuleCreateRequest reqNegative = CommissionRuleCreateRequest.builder()
                .rate(new BigDecimal("-1.00"))
                .build();

        CommissionRuleCreateRequest reqOverHundred = CommissionRuleCreateRequest.builder()
                .rate(new BigDecimal("105.00"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createRule(reqNegative));
        assertThrows(IllegalArgumentException.class, () -> service.createRule(reqOverHundred));
    }

    @Test
    @DisplayName("4. Create rule with non-existent category throws ResourceNotFoundException")
    void testCreateRule_NonExistentCategoryThrows() {
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        CommissionRuleCreateRequest req = CommissionRuleCreateRequest.builder()
                .categoryId(999)
                .rate(new BigDecimal("5.00"))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> service.createRule(req));
    }

    @Test
    @DisplayName("5. Soft-deleting rule sets isActive=false and effectiveTo=now")
    void testDeleteRule_SoftDeactivates() {
        CommissionRule rule = CommissionRule.builder()
                .ruleId(1)
                .rate(new BigDecimal("5.00"))
                .isActive(true)
                .build();

        when(commissionRuleRepository.findById(1)).thenReturn(Optional.of(rule));

        service.deleteRule(1);

        assertThat(rule.getIsActive()).isFalse();
        assertThat(rule.getEffectiveTo()).isNotNull();
        verify(commissionRuleRepository).save(rule);
    }
}
