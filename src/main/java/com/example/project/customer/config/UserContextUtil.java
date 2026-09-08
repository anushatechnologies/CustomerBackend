package com.example.project.customer.config;

import com.example.project.customer.security.FirebaseUserPrincipal;
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
     * Resolves the current internal user ID from the SecurityContext (FirebaseUserPrincipal),
     * HTTP request headers, query parameters, or fallback default.
     */
    public Integer getCurrentUserId() {
        // 1. Check SecurityContext for Firebase authenticated principal (Primary source of truth)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof FirebaseUserPrincipal fup) {
                if (fup.getInternalUserId() != null) {
                    return fup.getInternalUserId();
                }
            } else if (principal instanceof Number num) {
                return num.intValue();
            } else if (principal instanceof String str) {
                try {
                    return parseUserId(str);
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Check HTTP attributes (for backward compatibility in existing integration tests)
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

        return DEFAULT_USER_ID;
    }

    private Integer parseUserId(String value) {
        if (value.startsWith("user_")) {
            return Integer.parseInt(value.substring(5));
        }
        return Integer.parseInt(value);
    }
}
