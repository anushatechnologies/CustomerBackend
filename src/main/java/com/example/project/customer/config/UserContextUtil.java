package com.example.project.customer.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserContextUtil {

    public static final Integer DEFAULT_USER_ID = 101;

    /**
     * Resolves the current user ID from the HTTP request headers, query parameters,
     * security context, or fallback default.
     */
    public Integer getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserId = request.getHeader("X-User-Id");
            if (headerUserId != null && !headerUserId.trim().isEmpty()) {
                try {
                    return parseUserId(headerUserId.trim());
                } catch (Exception ignored) {
                }
            }

            String paramUserId = request.getParameter("userId");
            if (paramUserId != null && !paramUserId.trim().isEmpty()) {
                try {
                    return parseUserId(paramUserId.trim());
                } catch (Exception ignored) {
                }
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Number) {
                return ((Number) principal).intValue();
            }
            if (principal instanceof String) {
                try {
                    return parseUserId((String) principal);
                } catch (Exception ignored) {
                }
            }
        }

        return DEFAULT_USER_ID;
    }

    private Integer parseUserId(String value) {
        if (value.startsWith("user_")) {
            return Integer.parseInt(value.substring(5));
        }
        return Integer.parseInt(value);
    }
}
