package khtml.backend.alzi.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.auth.user.CustomUserDetails;
import khtml.backend.alzi.auth.user.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SecurityUtils {

    /**
     * 현재 인증된 사용자 정보를 가져옵니다.
     */
    public static User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // 인증 정보가 없는 경우
            if (authentication == null) {
                log.warn("Authentication is null - no token provided");
                throw new CustomException(ErrorCode.MISSING_TOKEN);
            }
            
            // 익명 사용자인 경우 (SecurityContext는 있지만 인증되지 않은 상태)
            if (!authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal()) ||
                authentication.getPrincipal() == null) {
                log.warn("User is not authenticated or anonymous user");
                throw new CustomException(ErrorCode.MISSING_TOKEN);
            }

            Object principal = authentication.getPrincipal();
            
            // Principal이 CustomUserDetails가 아닌 경우
            if (!(principal instanceof CustomUserDetails)) {
                log.warn("Principal is not instance of CustomUserDetails: {}", 
                    principal != null ? principal.getClass().getSimpleName() : "null");
                throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
            }

            CustomUserDetails userDetails = (CustomUserDetails) principal;
            User user = userDetails.getUser();
            
            if (user == null) {
                log.warn("User is null in CustomUserDetails");
                throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
            }
            
            return user;
            
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in getCurrentUser: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    /**
     * 현재 인증된 사용자의 userId를 가져옵니다.
     */
    public static String getCurrentUserIdString() {
        return getCurrentUser().getUserId();
    }

    /**
     * 현재 사용자가 특정 리소스에 대한 권한이 있는지 확인합니다.
     */
    public static void validateUserAccess(String resourceUserId) {
        String currentUserId = getCurrentUserIdString();
        if (!currentUserId.equals(resourceUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인합니다.
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || 
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal()) ||
                authentication.getPrincipal() == null) {
                return false;
            }
            
            return authentication.getPrincipal() instanceof CustomUserDetails;
            
        } catch (Exception e) {
            log.error("Error checking authentication status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 안전하게 현재 사용자 정보를 가져옵니다. (예외 없이)
     */
    public static User getCurrentUserSafely() {
        try {
            return getCurrentUser();
        } catch (CustomException e) {
            return null;
        }
    }

    /**
     * 현재 사용자의 ID를 안전하게 가져옵니다. (예외 없이)
     */
    public static String getCurrentUserIdSafely() {
        try {
            User user = getCurrentUser();
            return user != null ? user.getUserId() : null;
        } catch (CustomException e) {
            return null;
        }
    }

    /**
     * 현재 사용자의 userId를 안전하게 가져옵니다. (예외 없이)
     */
    public static String getCurrentUserIdStringSafely() {
        try {
            User user = getCurrentUser();
            return user != null ? user.getUserId() : null;
        } catch (CustomException e) {
            return null;
        }
    }
}
