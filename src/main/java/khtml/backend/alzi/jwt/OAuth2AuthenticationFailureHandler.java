package khtml.backend.alzi.jwt;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        
        log.error("OAuth2 인증 실패: {}", exception.getMessage(), exception);
        
        // 구체적인 오류 정보를 로그에 기록
        log.error("Request URI: {}", request.getRequestURI());
        log.error("Request Parameters: {}", request.getParameterMap());
        
        // 실패 시 로그인 페이지로 리다이렉트 (에러 파라미터와 함께)
        String errorMessage = "OAuth2 인증에 실패했습니다: " + exception.getMessage();
        response.sendRedirect("/login?error=" + java.net.URLEncoder.encode(errorMessage, "UTF-8"));
    }
}
