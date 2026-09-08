package com.example.project.customer.config;

import com.example.project.customer.security.FirebaseUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SellerContextUtil {

    public static final Integer DEFAULT_SELLER_ID = 1001;

    /**
     * Resolves the current seller ID from the SecurityContext (FirebaseUserPrincipal),
     * HTTP request headers, query parameters, or fallback default.
     */
    public Integer getCurrentSellerId() {
        // 1. Check SecurityContext for Firebase authenticated principal (Primary source of truth)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof FirebaseUserPrincipal fup) {
                if (fup.getSellerId() != null) {
                    return fup.getSellerId();
                }
            } else if (principal instanceof Number num) {
                return num.intValue();
            } else if (principal instanceof String str) {
                try {
                    return parseSellerId(str);
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Check HTTP attributes (for backward compatibility in existing tests)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerSellerId = request.getHeader("X-Seller-Id");
            if (headerSellerId != null && !headerSellerId.trim().isEmpty()) {
                try {
                    return parseSellerId(headerSellerId.trim());
                } catch (Exception ignored) {
                }
            }

            String paramSellerId = request.getParameter("sellerId");
            if (paramSellerId != null && !paramSellerId.trim().isEmpty()) {
                try {
                    return parseSellerId(paramSellerId.trim());
                } catch (Exception ignored) {
                }
            }
        }

        return DEFAULT_SELLER_ID;
    }

    public String getCurrentSellerIdString() {
        Integer id = getCurrentSellerId();
        return id != null ? "seller_" + id : "seller_" + DEFAULT_SELLER_ID;
    }

    private Integer parseSellerId(String value) {
        if (value.startsWith("seller_")) {
            return Integer.parseInt(value.substring(7));
        }
        return Integer.parseInt(value);
    }
}
