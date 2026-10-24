package com.example.project.customer.service;

import com.example.project.customer.dto.settlement.CommissionRuleCreateRequest;
import com.example.project.customer.dto.settlement.CommissionRuleResponse;
import com.example.project.customer.dto.settlement.CommissionRuleUpdateRequest;

import java.util.List;

public interface CommissionRuleService {
    List<CommissionRuleResponse> getRules(Integer categoryId, Boolean activeOnly);
    CommissionRuleResponse getRuleById(Integer ruleId);
    CommissionRuleResponse createRule(CommissionRuleCreateRequest request);
    CommissionRuleResponse updateRule(Integer ruleId, CommissionRuleUpdateRequest request);
    CommissionRuleResponse toggleRuleStatus(Integer ruleId, Boolean isActive);
    void deleteRule(Integer ruleId);
}
