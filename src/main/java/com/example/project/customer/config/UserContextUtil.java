package com.example.project.customer.config;

import com.example.project.customer.exception.UnauthorizedException;
import com.example.project.customer.security.FirebaseUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContextUtil {

    /**
     * Resolves the current internal user ID strictly from the authenticated SecurityContext (FirebaseUserPrincipal).
     * Header-based or request-parameter fallbacks are completely eliminated for security.
     */
    public Integer getCurrentUserId() {
        Integer userId = getOptionalCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required: No valid authenticated principal in SecurityContext.");
        }
        return userId;
    }

    /**
     * Returns the current internal user ID if authenticated, or null if unauthenticated.
     */
    public Integer getOptionalCurrentUserId() {
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
        return null;
    }

    private Integer parseUserId(String value) {
        if (value.startsWith("user_")) {
            return Integer.parseInt(value.substring(5));
        }
        return Integer.parseInt(value);
    }
}
