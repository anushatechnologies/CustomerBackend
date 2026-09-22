package com.example.project.customer.service;

import com.example.project.customer.dto.estimation.CandidateProductSummary;
import com.example.project.customer.dto.estimation.ExtractedRequirementItem;
import com.example.project.customer.entity.EstimationItem;
import com.example.project.customer.entity.MatchStatus;
import com.example.project.customer.entity.Product;
import com.example.project.customer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementMatchingServiceImpl implements RequirementMatchingService {

    private final ProductRepository productRepository;

    @Override
    public void matchRequirementItem(EstimationItem item, ExtractedRequirementItem req) {
        String reqName = req.getName() != null ? req.getName().trim() : "";
        String brandName = req.getBrand() != null ? req.getBrand().trim() : "";
        String spec = req.getSpecification() != null ? req.getSpecification().trim() : "";

        if (reqName.isEmpty()) {
            item.setMatchStatus(MatchStatus.NOT_FOUND);
            item.setIsAvailable(false);
            return;
        }

        List<Product> candidates = findCandidateProducts(reqName, brandName, spec);

        if (candidates.isEmpty()) {
            log.info("No candidate products found for requirement: {}", reqName);
            item.setMatchStatus(MatchStatus.NOT_FOUND);
            item.setIsAvailable(false);
            return;
        }

        List<ScoredProduct> scored = candidates.stream()
                .map(p -> new ScoredProduct(p, calculateScore(p, req)))
                .filter(sp -> sp.score > 10)
                .sorted(Comparator.comparingDouble((ScoredProduct sp) -> sp.score).reversed())
                .toList();

        if (scored.isEmpty()) {
            item.setMatchStatus(MatchStatus.NOT_FOUND);
            item.setIsAvailable(false);
            return;
        }

        if (scored.size() == 1 || (scored.get(0).score >= 80 && (scored.get(0).score - scored.get(1).score) >= 30)) {
            Product best = scored.get(0).product;
            log.info("Single high-confidence match found for '{}': id={}, title={}", reqName, best.getProductId(), best.getTitle());
            applySelectedProduct(item, best);
            item.setMatchStatus(MatchStatus.MATCHED);
        } else {
            log.info("Multiple candidate matches found for '{}': count={}", reqName, scored.size());
            List<Integer> candidateIds = scored.stream()
                    .limit(5)
                    .map(sp -> sp.product.getProductId())
                    .toList();

            item.setCandidateProductIds(candidateIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            applySelectedProduct(item, scored.get(0).product);
            item.setMatchStatus(MatchStatus.MULTIPLE_MATCHES);
        }
    }

    @Override
    public void applySelectedProduct(EstimationItem item, Product product) {
        if (product == null) {
            item.setMatchedProduct(null);
            item.setIsAvailable(false);
            item.setUnitPrice(null);
            item.setAvailableStock(0);
            return;
        }

        item.setMatchedProduct(product);
        item.setUnitPrice(product.getPrice());
        item.setGstRate(product.getGstRate() != null ? product.getGstRate() : BigDecimal.valueOf(18.0));
        item.setAvailableStock(product.getStockQty() != null ? product.getStockQty() : 0);
        item.setIsAvailable(product.isActive() && (product.getStockQty() != null && product.getStockQty() > 0));
    }

    @Override
    public List<CandidateProductSummary> getCandidateSummaries(String candidateProductIds) {
        if (candidateProductIds == null || candidateProductIds.isBlank()) {
            return Collections.emptyList();
        }

        List<Integer> ids = Arrays.stream(candidateProductIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                .map(Integer::parseInt)
                .toList();

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> products = productRepository.findAllById(ids);
        return products.stream().map(this::toCandidateSummary).toList();
    }

    @Override
    public CandidateProductSummary toCandidateSummary(Product p) {
        String brandName = p.getBrand() != null ? p.getBrand().getName() : null;
        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
        String subcategoryName = p.getSubcategory() != null ? p.getSubcategory().getName() : null;

        return CandidateProductSummary.builder()
                .productId(p.getProductId())
                .title(p.getTitle())
                .brandName(brandName)
                .categoryName(categoryName)
                .subcategoryName(subcategoryName)
                .price(p.getPrice())
                .mrp(p.getMrp())
                .unit(p.getUnit())
                .imageUrl(p.getImageUrl())
                .specifications(p.getSpecifications())
                .stockQty(p.getStockQty())
                .isAvailable(p.isActive() && (p.getStockQty() != null && p.getStockQty() > 0))
                .build();
    }

    private List<Product> findCandidateProducts(String reqName, String brandName, String spec) {
        List<String> keywords = extractKeywords(reqName);
        if (!brandName.isEmpty()) {
            keywords.add(brandName.toLowerCase(Locale.ROOT));
        }
        if (!spec.isEmpty()) {
            keywords.addAll(extractKeywords(spec));
        }

        Specification<Product> specPredicate = (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            var activePredicate = cb.isTrue(root.get("active"));
            var approvalPredicate = cb.or(
                    cb.equal(root.get("approvalStatus"), com.example.project.customer.entity.ApprovalStatus.APPROVED),
                    cb.isNull(root.get("approvalStatus"))
            );
            if (keywords.isEmpty()) {
                return cb.and(activePredicate, approvalPredicate);
            }

            var brandJoin = root.join("brand", jakarta.persistence.criteria.JoinType.LEFT);

            var orPredicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            for (String kw : keywords) {
                String pattern = "%" + kw + "%";
                orPredicates.add(cb.like(cb.lower(root.get("title")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("description")), pattern));
                orPredicates.add(cb.like(cb.lower(brandJoin.get("name")), pattern));
            }

            return cb.and(activePredicate, approvalPredicate, cb.or(orPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
        };

        return productRepository.findAll(specPredicate);
    }

    private List<String> extractKeywords(String text) {
        if (text == null) return Collections.emptyList();
        String[] tokens = text.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() >= 3 && !isStopWord(t)) {
                keywords.add(t);
            }
        }
        return keywords;
    }

    private boolean isStopWord(String word) {
        return List.of("the", "and", "for", "with", "req", "required", "need", "pack", "bags", "tonnes", "pcs", "pieces", "units").contains(word);
    }

    private double calculateScore(Product p, ExtractedRequirementItem req) {
        double score = 0;
        String title = p.getTitle().toLowerCase(Locale.ROOT);
        String reqName = req.getName().toLowerCase(Locale.ROOT);

        if (title.equals(reqName)) {
            score += 100;
        } else if (title.contains(reqName)) {
            score += 80;
        }

        List<String> reqTokens = extractKeywords(reqName);
        long matchedTokens = reqTokens.stream().filter(title::contains).count();
        if (!reqTokens.isEmpty()) {
            score += (matchedTokens * 40.0) / reqTokens.size();
        }

        if (req.getBrand() != null && !req.getBrand().isBlank() && p.getBrand() != null) {
            if (p.getBrand().getName().toLowerCase(Locale.ROOT).contains(req.getBrand().toLowerCase(Locale.ROOT))) {
                score += 35;
            }
        }

        if (req.getUnit() != null && p.getUnit() != null) {
            if (p.getUnit().equalsIgnoreCase(req.getUnit())) {
                score += 10;
            }
        }

        if (req.getSpecification() != null && !req.getSpecification().isBlank() && p.getSpecifications() != null) {
            String specLower = req.getSpecification().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : p.getSpecifications().entrySet()) {
                if (entry.getValue() != null && specLower.contains(entry.getValue().toLowerCase(Locale.ROOT))) {
                    score += 20;
                    break;
                }
            }
        }

        return score;
    }

    private record ScoredProduct(Product product, double score) {}
}
