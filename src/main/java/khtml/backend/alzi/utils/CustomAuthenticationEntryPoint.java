package khtml.backend.alzi.utils;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import khtml.backend.alzi.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 처리하는 클래스
 * 로그인 페이지로 리다이렉트하지 않고 JSON 에러 응답을 반환
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("Unauthorized access to: {} - {}", request.getRequestURI(), authException.getMessage());
        
        // 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 에러 응답 생성
        ApiResponse<Void> errorResponse = ApiResponse.failure(
            ErrorCode.UNAUTHORIZED.getMessage(),
            ErrorCode.UNAUTHORIZED.getCode()
        );
        
        // JSON으로 응답
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
